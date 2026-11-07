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
package pl.ks.profiling.safepoint.analyzer.commons.shared.safepoint.parser;

import pl.ks.profiling.safepoint.analyzer.commons.FileParser;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parses the output of -XX:+PrintSafepointStatistics -XX:PrintSafepointStatisticsCount=1 (JDK 8):
 * <pre>
 *          vmop                    [threads: total initially_running wait_to_block]    [time: spin block sync cleanup vmop] page_trap_count
 * 3.109: ParallelGCFailedAllocation       [      20          0              0    ]      [     0     0     0     0    12    ]  0
 * </pre>
 * The uptime is in seconds (with a dot or a comma, depending on the locale), times in ms.
 */
public class SafepointJdk8LogFileParser implements FileParser<SafepointLogFile> {
    private static final int SCALE = 10;
    private static final BigDecimal MS_IN_SECOND = new BigDecimal(1000);
    private static final Pattern STATISTICS_LINE_PATTERN = Pattern.compile(
            "^\\s*(\\d+[.,]\\d+): (.+?)\\s+\\[\\s*\\d+\\s+\\d+\\s+\\d+\\s*\\]" +
                    "\\s+\\[\\s*(\\d+)\\s+(\\d+)\\s+(\\d+)\\s+(\\d+)\\s+(\\d+)\\s*\\]\\s+\\d+");

    private boolean jdk8 = false;
    private BigDecimal lastUptimeMs = null;
    private BigDecimal lastStoppedMs = BigDecimal.ZERO;

    private SafepointLogFile safepointLogFile = new SafepointLogFile();

    @Override
    public void parseLine(String line) {
        if (line.contains("vmop") && line.contains("initially_running")) {
            jdk8 = true;
            return;
        }
        if (!jdk8) {
            return;
        }
        Matcher matcher = STATISTICS_LINE_PATTERN.matcher(line);
        if (matcher.find()) {
            addStatistics(matcher);
        }
    }

    @Override
    public SafepointLogFile fetchData() {
        safepointLogFile.parsingCompleted();
        return safepointLogFile;
    }

    private void addStatistics(Matcher matcher) {
        BigDecimal uptime = new BigDecimal(matcher.group(1).replace(',', '.'));
        String operationName = matcher.group(2).trim();
        // "sync" is the whole time to reach the safepoint (it includes spin and block)
        BigDecimal ttsMs = new BigDecimal(matcher.group(5));
        BigDecimal cleanupMs = new BigDecimal(matcher.group(6));
        BigDecimal vmopMs = new BigDecimal(matcher.group(7));
        BigDecimal stoppedMs = ttsMs.add(cleanupMs).add(vmopMs);
        BigDecimal uptimeMs = uptime.multiply(MS_IN_SECOND);

        // Application time: from the end of the previous safepoint to the start of this one
        BigDecimal applicationTimeMs = BigDecimal.ZERO;
        if (lastUptimeMs != null) {
            applicationTimeMs = uptimeMs.subtract(lastUptimeMs).subtract(lastStoppedMs).max(BigDecimal.ZERO);
        }

        safepointLogFile.newSafepoint(uptime);
        safepointLogFile.addAllData(
                toSeconds(ttsMs),
                toSeconds(stoppedMs),
                toSeconds(applicationTimeMs),
                operationName
        );
        lastUptimeMs = uptimeMs;
        lastStoppedMs = stoppedMs;
    }

    private static BigDecimal toSeconds(BigDecimal ms) {
        return ms.divide(MS_IN_SECOND, SCALE, RoundingMode.HALF_EVEN);
    }
}
