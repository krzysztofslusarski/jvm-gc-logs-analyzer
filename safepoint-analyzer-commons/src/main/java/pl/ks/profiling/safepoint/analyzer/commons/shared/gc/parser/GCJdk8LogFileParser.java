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
package pl.ks.profiling.safepoint.analyzer.commons.shared.gc.parser;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import pl.ks.profiling.safepoint.analyzer.commons.FileParser;
import pl.ks.profiling.safepoint.analyzer.commons.shared.ParserUtils;

public class GCJdk8LogFileParser implements FileParser<GCLogFile> {
    public static final BigDecimal D1024 = new BigDecimal(1024L);
    public static final BigDecimal JAVA_8_GB_MULTIPLIER = D1024;
    public static final BigDecimal JAVA_8_MB_MULTIPLIER = BigDecimal.ONE;
    public static final BigDecimal JAVA_8_KB_MULTIPLIER = BigDecimal.ONE.divide(D1024, 12, RoundingMode.HALF_EVEN);
    public static final BigDecimal JAVA_8_B_MULTIPLIER = JAVA_8_KB_MULTIPLIER.divide(D1024, 12, RoundingMode.HALF_EVEN);
    public static final BigDecimal TO_MS_MULTIPLIER = new BigDecimal(1000);

    private GCLogFile gcLogFile = new GCLogFile();
    private long java8SequenceId;

    public GCJdk8LogFileParser() {
    }

    @Override
    public void parseLine(String line) {
        parseJava8File(line);
    }

    @Override
    public GCLogFile fetchData() {
        gcLogFile.parsingCompleted();
        return gcLogFile;
    }

    private void parseJava8File(String line) {
        if (line.contains("GC pause")) {
            gcLogFile.newPhase(++java8SequenceId, getJava8Phase(line), getJava8TimeStamp(line));
            if (line.contains("secs")) {
                addJava8Time(java8SequenceId, line);
            }
        } else if (line.contains("Full GC")) {
            gcLogFile.newPhase(++java8SequenceId, "Full GC", getJava8TimeStamp(line));
            addJava8Time(java8SequenceId, line);
            addJava8Sizes(java8SequenceId, line, "Full GC", false, false);
        } else if (line.contains("secs") && !line.contains("Times")) {
            addJava8Time(java8SequenceId, line);
        } else if (line.contains("Heap: ") && line.contains("->")) {
            addJava8Sizes(java8SequenceId, line, "Heap", true, true);
        }
    }

    private void addJava8Time(long sequenceId, String line) {
        String[] splittedLine = line.split("(])|(\\[)");
        String timeStr = splittedLine[splittedLine.length - 1];
        BigDecimal time = ParserUtils.parseFirstBigDecimal(timeStr, 0).multiply(TO_MS_MULTIPLIER);
        gcLogFile.addTime(sequenceId, time);
    }

    private void addJava8Sizes(long sequenceId, String line, String startingString, boolean containsComma, boolean containsMaxHeapSizeBeforeGC) {
        Pattern pattern = containsComma ? Pattern.compile("\\d+,\\d+[BMKG]") : Pattern.compile("\\d+[BMKG]");
        String stringToSearch = line.substring(line.indexOf(startingString));
        Matcher matcher = pattern.matcher(stringToSearch);
        matcher.find();
        String before = matcher.group().replace(",", ".");
        if (containsMaxHeapSizeBeforeGC) {
            matcher.find();
        }
        matcher.find();
        String after = matcher.group().replace(",", ".");
        matcher.find();
        String heapSize = matcher.group().replace(",", ".");

        gcLogFile.addSizes(sequenceId, getSizeWithUnit(before).intValue(), getSizeWithUnit(after).intValue(), getSizeWithUnit(heapSize).intValue());
    }

    private BigDecimal getSizeWithUnit(String size) {
        BigDecimal multiplier = getMultiplier(size);
        return multiplier.multiply(new BigDecimal(size.substring(0, size.length() - 1))).setScale(2, RoundingMode.HALF_EVEN);
    }

    private BigDecimal getMultiplier(String size) {
        if (size.charAt(size.length() - 1) == 'K') {
            return JAVA_8_KB_MULTIPLIER;
        }
        if (size.charAt(size.length() - 1) == 'M') {
            return JAVA_8_MB_MULTIPLIER;
        }
        if (size.charAt(size.length() - 1) == 'G') {
            return JAVA_8_GB_MULTIPLIER;
        }
        return JAVA_8_B_MULTIPLIER;
    }

    private BigDecimal getJava8TimeStamp(String line) {
        Pattern pattern = Pattern.compile("\\d+,\\d+: ");
        Matcher matcher = pattern.matcher(line);
        matcher.find();
        return new BigDecimal(matcher.group().replace(',', '.').replace(": ", "").trim());
    }

    private String getJava8Phase(String line) {
        return line.substring(line.indexOf("("), line.lastIndexOf(")") + 1);
    }
}
