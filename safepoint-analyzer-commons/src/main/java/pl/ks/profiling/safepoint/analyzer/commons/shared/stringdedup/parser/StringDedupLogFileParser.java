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
package pl.ks.profiling.safepoint.analyzer.commons.shared.stringdedup.parser;

import java.math.BigDecimal;
import pl.ks.profiling.safepoint.analyzer.commons.FileParser;
import pl.ks.profiling.safepoint.analyzer.commons.shared.ParserUtils;

public class StringDedupLogFileParser implements FileParser<StringDedupLogFile> {
    public static final BigDecimal MB = new BigDecimal(1024);
    private final StringDedupLogFile logFile = new StringDedupLogFile();

    @Override
    public void parseLine(String line) {
        if (!line.contains("gc,stringdedup")) {
            return;
        }

        if (line.contains("Concurrent String Deduplication")) {
            logFile.newEntry(ParserUtils.getTimeStamp(line));
        } else if (line.contains("Last Exec")) {
            logFile.lastExecOccurred();
        } else if (line.contains("Total Exec")) {
            logFile.totalExecOccurred();
        } else if (line.contains("Deduplicated")) {
            int pos = line.indexOf("Deduplicated");
            long count = ParserUtils.parseFirstNumber(line, pos);
            int percentPos = line.indexOf("%)", pos);
            BigDecimal size = ParserUtils.parseFirstBigDecimal(line, percentPos);
            if (line.indexOf("M(", percentPos) >= 0) {
                size = size.multiply(MB);
            } else if (line.indexOf("G(", percentPos) >= 0) {
                size = size.multiply(MB).multiply(MB);
            }
            logFile.deduplicated(count, size);
        } else if (line.contains("Young")) {
            int pos = line.indexOf("Young");
            long count = ParserUtils.parseFirstNumber(line, pos);
            int percentPos = line.indexOf("%)", pos);
            BigDecimal size = ParserUtils.parseFirstBigDecimal(line, percentPos);
            if (line.indexOf("M(", percentPos) >= 0) {
                size = size.multiply(MB);
            } else if (line.indexOf("G(", percentPos) >= 0) {
                size = size.multiply(MB).multiply(MB);
            }
            logFile.young(count, size);
        } else if (line.contains("Old")) {
            int pos = line.indexOf("Old");
            long count = ParserUtils.parseFirstNumber(line, pos);
            int percentPos = line.indexOf("%)", pos);
            BigDecimal size = ParserUtils.parseFirstBigDecimal(line, percentPos);
            if (line.indexOf("M(", percentPos) >= 0) {
                size = size.multiply(MB);
            } else if (line.indexOf("G(", percentPos) >= 0) {
                size = size.multiply(MB).multiply(MB);
            }
            logFile.old(count, size);
        } else if (line.contains("New")) {
            int pos = line.indexOf("New");
            long count = ParserUtils.parseFirstNumber(line, pos);
            int percentPos = line.indexOf("%)", pos);
            BigDecimal size = ParserUtils.parseFirstBigDecimal(line, percentPos);
            if (line.indexOf("M", percentPos) >= 0) {
                size = size.multiply(MB);
            } else if (line.indexOf("G", percentPos) >= 0) {
                size = size.multiply(MB).multiply(MB);
            }
            logFile.newStrings(count, size);
        }
    }

    @Override
    public StringDedupLogFile fetchData() {
        return logFile;
    }
}
