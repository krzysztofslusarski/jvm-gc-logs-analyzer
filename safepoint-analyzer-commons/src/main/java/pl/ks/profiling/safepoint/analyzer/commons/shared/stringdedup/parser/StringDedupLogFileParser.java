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

import pl.ks.profiling.safepoint.analyzer.commons.FileParser;
import pl.ks.profiling.safepoint.analyzer.commons.shared.Consumer3;
import pl.ks.profiling.safepoint.analyzer.commons.shared.ParserUtils;

import java.math.BigDecimal;
import java.util.function.BiConsumer;

public class StringDedupLogFileParser implements FileParser<StringDedupLogFile> {
    private final StringDedupLogFile logFile = new StringDedupLogFile();

    private final TokenParser[] parsers = new TokenParser[]{
            new SimpleTokenParser("Concurrent String Deduplication", StringDedupLogFileParser::markDeduplicationStarted),
            new SimpleTokenParser("Last Exec", StringDedupLogFileParser::markLastExecSection),
            new SimpleTokenParser("Total Exec", StringDedupLogFileParser::markTotalExecOccurredSection),
            new SizeCountParser("Deduplicated", StringDedupLogFile::deduplicated),
            new SizeCountParser("Young", StringDedupLogFile::young),
            new SizeCountParser("Old", StringDedupLogFile::old),
            new SizeCountParser("New", StringDedupLogFile::newStrings, SizeCountParser.SIZE_MARKERS_WITH_PARENTHESIS)
    };

    private static void markDeduplicationStarted(StringDedupLogFile logFile, String line) {
        logFile.newEntry(ParserUtils.getTimeStamp(line));
    }

    private static void markLastExecSection(StringDedupLogFile logFile, String line) {
        logFile.lastExecOccurred();
    }

    private static void markTotalExecOccurredSection(StringDedupLogFile logFile, String line) {
        logFile.totalExecOccurred();
    }

    @Override
    public void parseLine(String line) {
        if (!isStringDeduplicationLine(line)) {
            return;
        }

        useFirstAccepting(line, parsers);
    }

    @Override
    public StringDedupLogFile fetchData() {
        return logFile;
    }

    private void useFirstAccepting(String line, TokenParser[] parsers) {
        for (TokenParser parser : parsers) {
            if (parser.accepts(line)) {
                parser.apply(logFile, line);
                return;
            }
        }
    }

    private boolean isStringDeduplicationLine(String line) {
        return line.contains("gc,stringdedup");
    }

    static abstract class TokenParser {
        protected String token;

        public TokenParser(String token) {
            this.token = token;
        }

        public boolean accepts(String line) {
            return line.contains(token);
        }

        abstract void apply(StringDedupLogFile logFile, String line);
    }

    static class SimpleTokenParser extends TokenParser {
        private final BiConsumer<StringDedupLogFile, String> applyFunction;

        public SimpleTokenParser(String token, BiConsumer<StringDedupLogFile, String> applyFunction) {
            super(token);
            this.applyFunction = applyFunction;
        }

        void apply(StringDedupLogFile logFile, String line) {
            applyFunction.accept(logFile, line);
        }
    }

    static class SizeCountParser extends TokenParser {
        private final String megaMarker;
        private final String gigaMarker;
        private final Consumer3<StringDedupLogFile, Long, BigDecimal> applyFunction;
        private final static boolean SIZE_MARKERS_WITHOUT_PARENTHESIS = false;
        public final static boolean SIZE_MARKERS_WITH_PARENTHESIS = true;

        public SizeCountParser(String token, Consumer3<StringDedupLogFile, Long, BigDecimal> applyFunction) {
            this(token, applyFunction, SIZE_MARKERS_WITHOUT_PARENTHESIS);
        }

        public SizeCountParser(String token, Consumer3<StringDedupLogFile, Long, BigDecimal> applyFunction, boolean paraSizeMarker) {
            super(token);
            this.token = token;
            this.applyFunction = applyFunction;

            if (paraSizeMarker) {
                this.megaMarker = "M(";
                this.gigaMarker = "G(";
            } else {
                this.megaMarker = "M";
                this.gigaMarker = "G";
            }
        }

        public void apply(StringDedupLogFile logFile, String line) {
            int tokenPosition = line.indexOf(token);
            long count = ParserUtils.parseFirstNumber(line, tokenPosition);
            int percentPos = getPercentPos(line, tokenPosition);
            BigDecimal sizeKb = getSizeInKiloBytes(line, percentPos);
            applyFunction.apply(logFile, count, sizeKb);
        }

        private int getPercentPos(String line, int tokenPosition) {
            return line.indexOf("%)", tokenPosition);
        }

        private BigDecimal getSizeInKiloBytes(String line, int percentPos) {
            BigDecimal size = ParserUtils.parseFirstBigDecimal(line, percentPos);
            if (containsMarker(line, percentPos, megaMarker)) {
                return UnitsConverter.megabytesToKiloBytes(size);
            } else if (containsMarker(line, percentPos, gigaMarker)) {
                return UnitsConverter.gigabytesToKiloBytes(size);
            } else {
                return size;
            }
        }

        private boolean containsMarker(String line, int percentPos, String marker) {
            return line.indexOf(marker, percentPos) >= 0;
        }
    }

}
