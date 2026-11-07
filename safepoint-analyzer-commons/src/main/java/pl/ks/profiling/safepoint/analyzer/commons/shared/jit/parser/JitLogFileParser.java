/*
 * Copyright 2020 Krzysztof Slusarski
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package pl.ks.profiling.safepoint.analyzer.commons.shared.jit.parser;

import pl.ks.profiling.safepoint.analyzer.commons.FileParser;
import pl.ks.profiling.safepoint.analyzer.commons.shared.ParserUtils;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class JitLogFileParser implements FileParser<JitLogFile> {
    /**
     * Compilation lines are printed as "%4d %c%c%c%c%c %d " (compile id, five flags, tier), e.g.
     * "  192     n 0       jdk.internal.misc.Unsafe::putObjectVolatile (native)".
     */
    private static final Pattern COMPILATION_PATTERN = Pattern.compile("^\\s*(\\d+) .{5} (\\d) ");

    private final Set<String> activeCompilations = new HashSet<>();

    private final JitLogFile jitLogFile = new JitLogFile();

    @Override
    public void parseLine(String line) {
        if (line.contains("codecache,sweep,start")) {
            jitLogFile.newCodeCacheSweeperActivity(ParserUtils.getTimeStamp(line));
        } else if (line.contains("codecache,sweep") && line.contains("size=")) {
            parseCodeCacheStats(line);
        } else if (line.contains("jit,compilation")) {
            // Logged at debug level up to JDK 24 and at info level since JDK 25
            parseJitCompilation(line);
        }
    }

    private void parseCodeCacheStats(String line) {
        BigDecimal timeStamp = ParserUtils.getTimeStamp(line);
        String segment = line.substring(line.lastIndexOf(']') + 2);
        segment = segment.substring(0, segment.indexOf(':'));
        long size = parseFirstNumber(line, line.indexOf("size="));
        long used = parseFirstNumber(line, line.indexOf("used="));
        long maxUsed = parseFirstNumber(line, line.indexOf("max_used="));
        jitLogFile.newCodeCacheStats(segment, timeStamp, size, maxUsed, used);
    }

    private static long parseFirstNumber(String line, int pos) {
        boolean started = false;
        long value = 0;
        for (int i = pos; i < line.length(); i++) {
            if (Character.isDigit(line.charAt(i))) {
                started = true;
                value *= 10;
                value += line.charAt(i) - '0';
            } else {
                if (started) {
                    return value;
                }
            }
        }
        return 0;
    }

    private void parseJitCompilation(String line) {
        BigDecimal timeStamp = ParserUtils.getTimeStamp(line);
        String withoutDecorators = line.substring(line.indexOf(']', line.indexOf("jit,compilation")) + 1);
        Matcher matcher = COMPILATION_PATTERN.matcher(withoutDecorators);
        if (!matcher.find()) {
            return;
        }
        String compileId = matcher.group(1);
        int tier = matcher.group(2).charAt(0) - '0';
        if (tier < 1 || tier > 4) {
            tier = 0;
        }
        // Native wrappers end with spaces: "... java.lang.Thread::sleep (native)   "
        String trimmed = withoutDecorators.trim();
        if (trimmed.contains("made not entrant")) {
            // "... (9 bytes)   made not entrant" (JDK 25+: "made not entrant: not used"). The same compilation can be
            // reported twice (e.g. deoptimization and class unloading), it is counted once.
            if (activeCompilations.remove(compileId)) {
                jitLogFile.compilationMadeNodEntrant(timeStamp, tier);
            }
        } else if (trimmed.lastIndexOf(')') == trimmed.length() - 1) {
            activeCompilations.add(compileId);
            jitLogFile.newCompilation(timeStamp, tier);
        }
        // Other lines, e.g. "made zombie" (JDK 19 and older), do not change the number of compiled methods
    }

    @Override
    public JitLogFile fetchData() {
        return jitLogFile;
    }
}
