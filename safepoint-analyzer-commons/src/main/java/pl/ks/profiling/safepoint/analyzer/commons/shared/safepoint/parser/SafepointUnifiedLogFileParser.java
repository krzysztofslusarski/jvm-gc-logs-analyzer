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
import pl.ks.profiling.safepoint.analyzer.commons.FileParser;
import pl.ks.profiling.safepoint.analyzer.commons.shared.ParserUtils;

public class SafepointUnifiedLogFileParser implements FileParser<SafepointLogFile> {
    private static final BigDecimal NS_TO_MS_DIVISIOR = new BigDecimal(1_000_000);
    private static final int SCALE = 10;

    private boolean waitForNext = true;

    private SafepointLogFile safepointLogFile = new SafepointLogFile();

    @Override
    public void parseLine(String line) {
        if (line.contains("Application time")) {
            safepointLogFile.newSafepoint(ParserUtils.getTimeStamp(line), parseApplicationTime(line));
            waitForNext = false;
        } else if (!waitForNext && line.contains("Entering safepoint region")) {
            addOperationName(line, safepointLogFile);
        } else if (!waitForNext && line.contains("Total time for which application threads were stopped")) {
            addTtsAndStoppedTime(line, safepointLogFile);
            waitForNext = true;
        } else if (line.contains("Reaching safepoint")) {
            safepointLogFile.newSafepoint(ParserUtils.getTimeStamp(line));
            addJava13OneLine(line, safepointLogFile);
            waitForNext = false;
        }
    }

    @Override
    public SafepointLogFile fetchData() {
        safepointLogFile.parsingCompleted();
        return safepointLogFile;
    }

    private static void addJava13OneLine(String line, SafepointLogFile current) {
        String name = line
                .replaceFirst(".* Safepoint \"", "")
                .replaceFirst("\", Time since.*", "")
                .trim();
        String tts = line
                .replaceFirst(".* Reaching safepoint: ", "")
                .replaceFirst(" ns, At safepoint.*", "")
                .trim();
        String stopped = line
                .replaceFirst(".*Total: ", "")
                .replaceFirst(" ns", "")
                .trim();
        String appTime = line
                .replaceFirst(".*Time since last: ", "")
                .replaceFirst(" ns, Reaching safepoint.*", "")
                .trim();
        current.addAllData(
                new BigDecimal(tts)
                        .divide(NS_TO_MS_DIVISIOR, SCALE, RoundingMode.HALF_EVEN)
                        .divide(SafepointStatsCreator.TO_MS_MULTIPLIER, SCALE, RoundingMode.HALF_EVEN),
                new BigDecimal(stopped)
                        .divide(NS_TO_MS_DIVISIOR, SCALE, RoundingMode.HALF_EVEN)
                        .divide(SafepointStatsCreator.TO_MS_MULTIPLIER, SCALE, RoundingMode.HALF_EVEN),
                new BigDecimal(appTime)
                        .divide(NS_TO_MS_DIVISIOR, SCALE, RoundingMode.HALF_EVEN)
                        .divide(SafepointStatsCreator.TO_MS_MULTIPLIER, SCALE, RoundingMode.HALF_EVEN),
                name
        );
    }

    private static void addTtsAndStoppedTime(String line, SafepointLogFile current) {
        String tts = line
                .replaceFirst(".*Stopping threads took: ", "")
                .replace(" seconds", "")
                .replace(',', '.')
                .trim();
        String stopped = line
                .replaceFirst(".*Total time for which application threads were stopped: ", "")
                .replaceFirst(" seconds, Stopping threads.*", "")
                .replace(',', '.')
                .trim();

        current.addTimeToSafepointAndStoppedTime(new BigDecimal(tts), new BigDecimal(stopped));
    }

    private static void addOperationName(String line, SafepointLogFile safepointLogFile) {
        String name = line
                .replaceFirst(".*Entering safepoint region: ", "")
                .trim();
        safepointLogFile.addOperationName(name);
    }

    private static BigDecimal parseApplicationTime(String line) {
        String appTime = line
                .replaceFirst(".*Application time: ", "")
                .replace(" seconds", "")
                .replace(',', '.')
                .trim();
        return new BigDecimal(appTime);
    }
}
