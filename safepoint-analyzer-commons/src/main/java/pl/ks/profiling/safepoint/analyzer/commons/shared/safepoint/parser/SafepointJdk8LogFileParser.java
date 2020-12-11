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

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.stream.Stream;
import pl.ks.profiling.safepoint.analyzer.commons.FileParser;
import pl.ks.profiling.safepoint.analyzer.commons.shared.ParserUtils;

public class SafepointJdk8LogFileParser implements FileParser<SafepointLogFile> {
    private static final int SCALE = 10;
    private static final String JDK8_REGEX = ".*: .*\\[.*\\] *.*\\[.*\\].*";

    private boolean waitForNext = true;
    private boolean jdk8 = false;
    private BigDecimal lastAppTime = null;

    private SafepointLogFile safepointLogFile = new SafepointLogFile();

    @Override
    public void parseLine(String line) {
        if (line.contains("vmop") && line.contains("initially_running")) {
            jdk8 = true;
        } else if (jdk8 && line.matches(JDK8_REGEX) && line.split("\\[").length == 3) {
            safepointLogFile.newSafepoint(ParserUtils.getTimeStamp(line));
            lastAppTime = addJava8OneLine(line, safepointLogFile, lastAppTime);
            waitForNext = false;
        }
    }

    @Override
    public SafepointLogFile fetchData() {
        safepointLogFile.parsingCompleted();
        return safepointLogFile;
    }

    //[1,060s][info][safepoint] Safepoint "Cleanup", Time since last: 1000140224 ns, Reaching safepoint: 196482 ns, At safepoint: 16740 ns, Total: 213222 ns

    private static BigDecimal addJava8OneLine(String line, SafepointLogFile current, BigDecimal lastAppTime) {
        String[] splittedLine = line.split(" ");
        String[] lineNoBlank = Stream.of(splittedLine)
                .filter(s -> s.length() > 0)
                .toArray(String[]::new);
        StringBuilder operationName = new StringBuilder();

        int index = 0;
        BigDecimal currentAppTime = new BigDecimal(lineNoBlank[index++].replaceAll("\\.", "").replaceAll(",", "").replaceAll(":", ""));
        while (!"[".equals(lineNoBlank[index])) {
            operationName.append(lineNoBlank[index]).append(" ");
            index++;
        }

        index++;
        while (!"[".equals(lineNoBlank[index])) {
            index++;
        }

        BigDecimal tts = new BigDecimal(lineNoBlank[index + 3]);
        BigDecimal stoppedTime = new BigDecimal(lineNoBlank[index + 5]).add(tts);
        current.addAllData(
                tts.divide(SafepointStatsCreator.TO_MS_MULTIPLIER, SCALE, RoundingMode.HALF_EVEN),
                stoppedTime.divide(SafepointStatsCreator.TO_MS_MULTIPLIER, SCALE, RoundingMode.HALF_EVEN),
                lastAppTime == null ? BigDecimal.ZERO : currentAppTime.subtract(lastAppTime).subtract(stoppedTime)
                        .divide(SafepointStatsCreator.TO_MS_MULTIPLIER, SCALE, RoundingMode.HALF_EVEN),
                operationName.toString().trim()
        );

        return currentAppTime;
    }
}
