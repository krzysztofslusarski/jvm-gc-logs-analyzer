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

import pl.ks.profiling.safepoint.analyzer.commons.FileParser;
import pl.ks.profiling.safepoint.analyzer.commons.shared.ParserUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class GCJdk8LogFileParser implements FileParser<GCLogFile> {
    public static final BigDecimal D1024 = new BigDecimal(1024L);
    public static final BigDecimal JAVA_8_GB_MULTIPLIER = D1024;
    public static final BigDecimal JAVA_8_MB_MULTIPLIER = BigDecimal.ONE;
    public static final BigDecimal JAVA_8_KB_MULTIPLIER = BigDecimal.ONE.divide(D1024, 12, RoundingMode.HALF_EVEN);
    public static final BigDecimal JAVA_8_B_MULTIPLIER = JAVA_8_KB_MULTIPLIER.divide(D1024, 12, RoundingMode.HALF_EVEN);
    public static final BigDecimal TO_MS_MULTIPLIER = new BigDecimal(1000);

    // Uptime printed by -XX:+PrintGCTimeStamps: "12.345: " (or "12,345: " depending on the locale)
    private static final Pattern UPTIME_PATTERN = Pattern.compile("(\\d+[.,]\\d+): ");
    private static final Pattern SECONDS_PATTERN = Pattern.compile("\\d+[.,]\\d+ secs");
    // Generation details inside a Parallel/Serial/CMS record: "[PSYoungGen: 130101K->16976K(153088K)]"
    private static final Pattern GENERATION_SECTION_PATTERN = Pattern.compile("\\[[A-Za-z ]+: [^\\]]*\\]");
    private static final String SIZE = "\\d+(?:[.,]\\d+)?[BKMG]";
    // Whole heap: "130101K->16984K(502784K)"
    private static final Pattern HEAP_TRANSITION_PATTERN = Pattern.compile("(" + SIZE + ")->(" + SIZE + ")\\((" + SIZE + ")\\)");
    // G1 details: "Heap: 25600.0K(512.0M)->4404.5K(512.0M)"
    private static final Pattern G1_HEAP_PATTERN = Pattern.compile("Heap: (" + SIZE + ")\\(" + SIZE + "\\)->(" + SIZE + ")\\((" + SIZE + ")\\)");

    private GCLogFile gcLogFile = new GCLogFile();
    private long java8SequenceId;
    private BigDecimal lastTimeStamp = BigDecimal.ZERO;
    private BigDecimal concurrentCycleStart;
    private long concurrentCycleId;
    // Between the first line of a pause record and its "[Times: ...]" part
    private boolean recordOpen;

    @Override
    public void parseLine(String line) {
        parseJava8File(line);
    }

    @Override
    public GCLogFile fetchData() {
        gcLogFile.parsingCompleted();
        return gcLogFile;
    }

    /**
     * JDK 8 GC records span several lines when -XX:+PrintTenuringDistribution or G1 details are enabled, e.g.
     * <pre>
     * 3.110: [GC (Allocation Failure)
     * Desired survivor size 22020096 bytes, new threshold 7 (max 15)
     * [PSYoungGen: 130101K->16976K(153088K)] 130101K->16984K(502784K), 0.0118661 secs] [Times: ...]
     * </pre>
     * A record starts with its first line and ends with the "[Times: ...]" part.
     */
    private void parseJava8File(String line) {
        if (line.contains("[GC concurrent-")) {
            // Concurrent G1 phases may be printed in the middle of another record
            parseConcurrentPhase(line);
        } else if (line.contains("GC pause")) {
            String phase = getJava8Phase(line);
            newRecord(phase.replace(" (to-space exhausted)", ""), getJava8TimeStamp(line));
            continueRecord(line);
        } else if (line.contains("[Full GC")) {
            // Named like in unified logging, e.g. "Pause Full (Ergonomics)"
            newRecord("Pause Full" + causeAfter(line, "[Full GC"), getJava8TimeStamp(line));
            continueRecord(line);
        } else if (line.contains("[GC (")) {
            // Parallel/Serial/CMS young collection, e.g. "Pause Young (Allocation Failure)"
            newRecord("Pause Young" + causeAfter(line, "[GC"), getJava8TimeStamp(line));
            continueRecord(line);
        } else if (line.contains("GC cleanup")) {
            BigDecimal timeStamp = getJava8TimeStamp(line);
            newRecord("Pause Cleanup", timeStamp);
            BigDecimal time = continueRecord(line);
            // The cleanup pause ends the concurrent cycle
            finishConcurrentCycle(time == null ? timeStamp : timeStamp.add(time.divide(TO_MS_MULTIPLIER, 6, RoundingMode.HALF_EVEN)));
        } else if (line.contains("GC remark")) {
            newRecord("Pause Remark", getJava8TimeStamp(line));
            continueRecord(line);
        } else if (!recordOpen) {
            // Outside of pause records only concurrent output is printed (e.g. string deduplication statistics,
            // whose "   [Last Exec: ...]" lines look like G1 phases)
            return;
        } else if (line.contains("Heap: ") && line.contains("->")) {
            addJava8G1HeapSizes(java8SequenceId, line);
        } else if (line.startsWith("Desired survivor size")) {
            addJava8SurvivorStats(java8SequenceId, line);
        } else if (line.startsWith("- age")) {
            addJava8AgeCount(java8SequenceId, line);
        } else if (line.startsWith("   [") && !line.contains("->")) {
            addJava8PhaseYoungAndMixed(java8SequenceId, line, gcLogFile, false);
        } else if (line.startsWith("      [") && !line.contains("GC Worker Start") && !line.contains("GC Worker End")) {
            addJava8PhaseYoungAndMixed(java8SequenceId, line, gcLogFile, true);
        } else if (line.contains("secs") || line.contains("Times")) {
            // Continuation of the current record: ", 0.0024560 secs]" or "[PSYoungGen: ...] ... secs] [Times: ...]"
            continueRecord(line);
        }
    }

    /**
     * Adds what the line contains to the current record: heap sizes, duration, to-space exhaustion, and finishes the
     * record when its "[Times: ...]" part is present.
     *
     * @return the duration in ms if the line contains it
     */
    private BigDecimal continueRecord(String line) {
        if (line.contains("(to-space exhausted)")) {
            gcLogFile.toSpaceExhausted(java8SequenceId);
        }
        BigDecimal time = addJava8Time(java8SequenceId, line);
        if (line.contains("->") && !line.contains("Heap: ")) {
            addJava8HeapSizes(java8SequenceId, line);
        }
        if (line.contains("[Times")) {
            gcLogFile.finishCycle(java8SequenceId);
            recordOpen = false;
        }
        return time;
    }

    private void newRecord(String phase, BigDecimal timeStamp) {
        gcLogFile.newPhase(++java8SequenceId, phase, timeStamp);
        recordOpen = true;
    }

    /**
     * G1 concurrent cycle: it starts with the root region scan after an initial-mark pause and ends with the
     * cleanup pause, or when marking is aborted by a full collection.
     */
    private void parseConcurrentPhase(String line) {
        if (line.contains("concurrent-root-region-scan-start")) {
            concurrentCycleId = java8SequenceId;
            concurrentCycleStart = getJava8TimeStamp(line);
            gcLogFile.concurrentCycleStarted(concurrentCycleId, GCLogConcurrentCycleEntry.CONCURRENT_MARK);
        } else if (line.contains("concurrent-mark-abort")) {
            finishConcurrentCycle(getJava8TimeStamp(line));
        }
    }

    private void finishConcurrentCycle(BigDecimal endTimeStamp) {
        if (concurrentCycleStart == null) {
            return;
        }
        BigDecimal duration = endTimeStamp.subtract(concurrentCycleStart).multiply(TO_MS_MULTIPLIER);
        gcLogFile.concurrentCycleFinished(concurrentCycleId, GCLogConcurrentCycleEntry.CONCURRENT_MARK, duration);
        concurrentCycleStart = null;
    }

    private void addJava8PhaseYoungAndMixed(Long sequenceId, String line, GCLogFile gcLogFile, boolean subSubPhase) {
        if (line.contains("Max:") && !line.contains("(ms)")) {
            // Counters such as "Processed Buffers" or "Termination Attempts", not times
            return;
        }
        String phase = line.replaceFirst(".*\\[", "").replaceFirst(":.*", "");
        if (subSubPhase) {
            phase = "|______" + phase;
        }
        if (line.contains("Max:")) {
            String time = line.replaceFirst(".*Max:", "").replaceFirst(", Diff.*", "").trim().replace(',', '.');
            gcLogFile.addSubPhaseTime(sequenceId, phase, new BigDecimal(time));
        } else {
            String time = line.replaceFirst("ms.*", "").replaceFirst(".*:", "").trim().replace(',', '.');
            gcLogFile.addSubPhaseTime(sequenceId, phase, new BigDecimal(time));
        }
    }

    private void addJava8AgeCount(Long sequenceId, String line) {
        String ageStr = line
                .replaceFirst("- age", "")
                .replaceFirst(":.*", "")
                .trim();
        String sizeStr = line
                .replaceFirst(".*:", "")
                .replaceFirst("bytes.*", "")
                .trim();

        int age = Integer.parseInt(ageStr);
        long size = Long.parseLong(sizeStr);

        gcLogFile.addAgeWithSize(sequenceId, age, size);
    }

    private void addJava8SurvivorStats(Long sequenceId, String line) {
        int desiredSizePos = line.indexOf("Desired survivor size");
        int newThresholdPos = line.indexOf("new threshold", desiredSizePos);
        int maxThresholdPos = line.indexOf("max", newThresholdPos);

        long desiredSize = ParserUtils.parseFirstNumber(line, desiredSizePos);
        long newThreshold = ParserUtils.parseFirstNumber(line, newThresholdPos);
        long maxThreshold = ParserUtils.parseFirstNumber(line, maxThresholdPos);

        gcLogFile.addSurvivorStats(sequenceId, desiredSize, newThreshold, maxThreshold);
    }

    /**
     * Adds the last "N secs" of the line (the duration of the whole record; nested phases come earlier).
     *
     * @return the duration in ms or {@code null} if the line does not contain it
     */
    private BigDecimal addJava8Time(long sequenceId, String line) {
        Matcher matcher = SECONDS_PATTERN.matcher(line.replaceFirst("\\[Times.*", ""));
        String timeStr = null;
        while (matcher.find()) {
            timeStr = matcher.group();
        }
        if (timeStr == null) {
            return null;
        }
        // Full precision: "0.0118661 secs" is 11.8661 ms (ParserUtils.parseFirstBigDecimal rounds to 0.01)
        BigDecimal time = new BigDecimal(timeStr.replace(" secs", "").replace(',', '.')).multiply(TO_MS_MULTIPLIER);
        gcLogFile.addTime(sequenceId, time);
        return time;
    }

    /**
     * Whole heap transition of a record, e.g. "[GC (Allocation Failure) [PSYoungGen: 130101K->16976K(153088K)]
     * 130101K->16984K(502784K), 0.0118661 secs]". Generation sections are skipped.
     */
    private void addJava8HeapSizes(long sequenceId, String line) {
        Matcher matcher = HEAP_TRANSITION_PATTERN.matcher(GENERATION_SECTION_PATTERN.matcher(line).replaceAll(""));
        if (matcher.find()) {
            gcLogFile.addSizes(sequenceId, getSizeWithUnit(matcher.group(1)).intValue(),
                    getSizeWithUnit(matcher.group(2)).intValue(), getSizeWithUnit(matcher.group(3)).intValue());
        }
    }

    /**
     * G1 details line: "[Eden: ... Survivors: ... Heap: 25600.0K(512.0M)->4404.5K(512.0M)]".
     */
    private void addJava8G1HeapSizes(long sequenceId, String line) {
        Matcher matcher = G1_HEAP_PATTERN.matcher(line);
        if (matcher.find()) {
            gcLogFile.addSizes(sequenceId, getSizeWithUnit(matcher.group(1)).intValue(),
                    getSizeWithUnit(matcher.group(2)).intValue(), getSizeWithUnit(matcher.group(3)).intValue());
        }
    }

    private BigDecimal getSizeWithUnit(String size) {
        BigDecimal multiplier = getMultiplier(size);
        return multiplier.multiply(new BigDecimal(size.substring(0, size.length() - 1).replace(',', '.'))).setScale(2, RoundingMode.HALF_EVEN);
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

    /**
     * Uptime of the line. With -XX:+PrintGCDateStamps it follows the date ("2026-11-07T10:37:02.012+0100: 3.110: ");
     * the date itself never matches, because its fraction is followed by the time zone. Lines without the uptime
     * (-XX:+PrintGCTimeStamps disabled) get the previous one.
     */
    private BigDecimal getJava8TimeStamp(String line) {
        Matcher matcher = UPTIME_PATTERN.matcher(line);
        if (matcher.find()) {
            lastTimeStamp = new BigDecimal(matcher.group(1).replace(',', '.'));
        }
        return lastTimeStamp;
    }

    /**
     * The parenthesized cause following the marker, e.g. " (System.gc())" for "[Full GC (System.gc()) [PSYoungGen:",
     * or an empty string.
     */
    private static String causeAfter(String line, String marker) {
        int start = line.indexOf('(', line.indexOf(marker));
        if (start < 0) {
            return "";
        }
        int depth = 0;
        for (int i = start; i < line.length(); i++) {
            if (line.charAt(i) == '(') {
                depth++;
            } else if (line.charAt(i) == ')' && --depth == 0) {
                return " " + line.substring(start, i + 1);
            }
        }
        return "";
    }

    private String getJava8Phase(String line) {
        return line.substring(line.indexOf("("), line.lastIndexOf(")") + 1);
    }
}
