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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class GCUnifiedLogFileParser implements FileParser<GCLogFile> {
    private final GCLogFile gcLogFile = new GCLogFile();
    private String lastRegion;
    private final GCCollectorType collectorType;
    private Long currentCycleId = null;

    public GCUnifiedLogFileParser(GCCollectorType collectorType) {
        this.collectorType = collectorType;
    }

    public interface TriConsumer<A, B, C> {
        void accept(A a, B b, C c);
    }

    static class GcLineParser {
        private final List<String> included;
        private final List<String> excluded;
        private final TriConsumer<GCLogFile, Long, String> parseLine;

        public GcLineParser(List<String> included, List<String> excluded, TriConsumer<GCLogFile, Long, String> parseLine) {
            this.included = included;
            this.excluded = excluded;
            this.parseLine = parseLine;
        }

        public boolean accepts(String line) {
            return includesAll(line) && excludesAll(line);
        }

        public void apply(GCLogFile logFile, String line) {
            parseLine.accept(logFile, getSequenceId(line), line);
        }

        private boolean includesAll(String line) {
            return included.stream().allMatch(line::contains);
        }

        private boolean excludesAll(String line) {
            if (excluded != null) {
                return excluded.stream().noneMatch(line::contains);
            } else {
                return true;
            }
        }

        private Long getSequenceId(String line) {
            Pattern pattern = Pattern.compile("\\(\\d+\\)");
            Matcher matcher = pattern.matcher(line);
            matcher.find();
            return Long.valueOf(matcher.group().replace("(", "").replace(")", "").trim());
        }
    }

    private final List<GcLineParser> parsers = List.of(
            new GcLineParser(includes("gc,start"), excludes(), this::gcStart),
            new GcLineParser(includes("gc ", "Collection"), excludes("gc,start", "gc,phases", "->"), this::zgcStart),
            new GcLineParser(includes("gc ", "Concurrent Cycle", "ms"), excludes(), this::addConcurrentCycleDataIfPresent),
            new GcLineParser(includes("gc ", "Concurrent Mark Cycle", "ms"), excludes(), this::addConcurrentCycleDataIfPresent),
            new GcLineParser(includes("gc,phases", "ms", ")   "), excludes(")       ", "Queue Fixup", "Table Fixup"), this::addPhaseYoungAndMixed),
            new GcLineParser(includes("gc,phases", "ms"), excludes(")  "), this::addPhaseConcurrentSTW),
            new GcLineParser(includes("gc ", "->"), excludes(), this::addSizesAndTime),
            new GcLineParser(includes("gc ", "ms"), excludes("gc,start", "gc,phases", "->", "gc,heap", "gc,humongous",
                    "gc,age", "Concurrent Cycle", "Concurrent Mark Cycle", "- age", "To-space exhausted"),
                    this::addShenandoahPhaseTime),
            new GcLineParser(includes("regions", "gc,heap", "info"), excludes(), this::addRegionsCounts),
            new GcLineParser(includes("gc,heap", "trace"), excludes(), this::addRegionsSizes),
            new GcLineParser(includes("gc,humongous", "debug"), excludes(), this::addHumongous),
            new GcLineParser(includes("- age"), excludes(), this::addAgeCount),
            new GcLineParser(includes("gc,age", "debug", "Desired survivor size"), excludes(), this::addSurvivorStats),
            new GcLineParser(includes("To-space exhausted"), excludes(), this::toSpaceExhausted)
    );

    private static List<String> includes(String... included) {
        return new ArrayList<>(Arrays.asList(included));
    }

    private static List<String> excludes(String... excluded) {
        return new ArrayList<>(Arrays.asList(excluded));
    }

    public GCUnifiedLogFileParser() {
        this(GCCollectorType.G1_AND_PARALLEL);
    }

    @Override
    public void parseLine(String line) {
        if (isGcLog(line)) {
            useFirstAccepting(line, parsers);
        }
    }

    private boolean isGcLog(String line) {
        return line.contains("gc") && line.contains("GC(");
    }

    private void useFirstAccepting(String line, List<GcLineParser> parsers) {
        for (GcLineParser parser : parsers) {
            if (parser.accepts(line)) {
                parser.apply(this.gcLogFile, line);
                return;
            }
        }
    }

    private void gcStart(GCLogFile gcLogFile, Long sequenceId, String line) {
        if (collectorType == GCCollectorType.SHENANDOAH) {
            if (currentCycleId != null && !currentCycleId.equals(sequenceId)) {
                gcLogFile.finishCycle(currentCycleId);
            }
            if (currentCycleId == null || !currentCycleId.equals(sequenceId)) {
                gcLogFile.newPhase(sequenceId, getPhase(line), ParserUtils.getTimeStamp(line));
                currentCycleId = sequenceId;
            }
            return;
        }
        gcLogFile.newPhase(sequenceId, getPhase(line), ParserUtils.getTimeStamp(line));
    }

    private void zgcStart(GCLogFile gcLogFile, Long sequenceId, String line) {
        if (collectorType != GCCollectorType.ZGC) {
            return;
        }
        if (currentCycleId != null && !currentCycleId.equals(sequenceId)) {
            gcLogFile.finishCycle(currentCycleId);
        }
        if (currentCycleId == null || !currentCycleId.equals(sequenceId)) {
            gcLogFile.newPhase(sequenceId, getPhase(line), ParserUtils.getTimeStamp(line));
            currentCycleId = sequenceId;
        }
    }

    private void addConcurrentCycleDataIfPresent(GCLogFile gcLogFile, Long sequenceId, String line) {
        String time = getTime(line);
        gcLogFile.newConcurrentCycle(sequenceId, new BigDecimal(time));
    }

    @Override
    public GCLogFile fetchData() {
        gcLogFile.parsingCompleted();
        return gcLogFile;
    }

    private void addShenandoahPhaseTime(GCLogFile gcLogFile, Long sequenceId, String line) {
        if (collectorType != GCCollectorType.SHENANDOAH) {
            return;
        }
        String phaseWithTime = line.replaceFirst(".*GC\\(\\d+\\)", "").trim();
        String time = getTime(phaseWithTime);
        int timeIndex = phaseWithTime.indexOf(time);
        String phase = phaseWithTime.substring(0, timeIndex).trim();
        gcLogFile.addSubPhaseTime(sequenceId, phase, new BigDecimal(time));
    }

    private void addPhaseConcurrentSTW(GCLogFile gcLogFile, Long sequenceId, String line) {
        String phaseWithTime = line.replaceFirst(".*GC\\(\\d+\\)", "").trim();
        String time = getTime(phaseWithTime);
        int timeIndex = phaseWithTime.indexOf(time);
        String phase = phaseWithTime.substring(0, timeIndex).trim();
        gcLogFile.addSubPhaseTime(sequenceId, phase, new BigDecimal(time));
    }

    private String getTime(String line) {
        if (line.contains("ms")) {
            Pattern timeExtractorPatter = Pattern.compile("\\d+.\\d+(\\s*)?ms");
            Matcher matcher = timeExtractorPatter.matcher(line);
            matcher.find();
            String timePart = matcher.group();
            return timePart.replaceAll("ms", "").replaceAll(",", ".").trim();
        }
        Pattern timeExtractorPatter = Pattern.compile("\\d+.\\d+(\\s*)?s");
        Matcher matcher = timeExtractorPatter.matcher(line);
        String timePart = null;
        while (matcher.find()) {
            timePart = matcher.group();
        }

        return timePart.replaceAll("s", "").replaceAll(",", ".").replaceAll("\\.","").trim();
    }

    private void addPhaseYoungAndMixed(GCLogFile gcLogFile, Long sequenceId, String line) {
        String phase = line.replaceFirst(".*GC\\(\\d+\\)", "").replaceFirst(":.*", "").replace("   ", "").replaceAll("  ", "|______").replace(" (ms)", "");
        if (line.contains("Max:")) {
            String time = line.replaceFirst(".*Max:", "").replaceFirst(",.*", "").trim().replace(',', '.');
            gcLogFile.addSubPhaseTime(sequenceId, phase, new BigDecimal(time));
        } else if (line.contains("skipped")) {
            gcLogFile.addSubPhaseTime(sequenceId, phase, BigDecimal.ZERO);
        } else {
            String time = line.replaceFirst(".*:", "").replaceFirst("ms", "").trim().replace(',', '.');
            gcLogFile.addSubPhaseTime(sequenceId, phase, new BigDecimal(time));
        }
    }

    private void addSizesAndTime(GCLogFile gcLogFile, Long sequenceId, String line) {
        Pattern pattern = Pattern.compile("\\d+M");
        Matcher matcher = pattern.matcher(line);
        matcher.find();
        String before = matcher.group().replace("M", "");
        matcher.find();
        String after = matcher.group().replace("M", "");
        matcher.find();
        String heapSize =
                line.contains("%") ? "-1" : matcher.group().replace("M", "");
        String time = getTime(line);

        if (collectorType == GCCollectorType.SHENANDOAH || collectorType == GCCollectorType.ZGC) {
            gcLogFile.addSizes(sequenceId, Integer.parseInt(before), Integer.parseInt(after), Integer.parseInt(heapSize));
            String phaseName = line.replaceFirst(".*GC\\(\\d+\\)", "").trim()
                    .replaceFirst("\\s*\\d+M.*->.*", "").trim();
            gcLogFile.addSubPhaseTime(sequenceId, phaseName, new BigDecimal(time));
        } else {
            gcLogFile.addSizesAndTime(sequenceId, Integer.parseInt(before), Integer.parseInt(after), Integer.parseInt(heapSize), new BigDecimal(time));
        }
    }

    private void addSurvivorStats(GCLogFile gcLogFile, Long sequenceId, String line) {
        int desiredSizePos = line.indexOf("Desired survivor size");
        int newThresholdPos = line.indexOf("new threshold", desiredSizePos);
        int maxThresholdPos = line.indexOf("max threshold", newThresholdPos);

        long desiredSize = ParserUtils.parseFirstNumber(line, desiredSizePos);
        long newThreshold = ParserUtils.parseFirstNumber(line, newThresholdPos);
        long maxThreshold = ParserUtils.parseFirstNumber(line, maxThresholdPos);

        gcLogFile.addSurvivorStats(sequenceId, desiredSize, newThreshold, maxThreshold);
    }

    private void toSpaceExhausted(GCLogFile gcLogFile, Long sequenceId, String line) {
        gcLogFile.toSpaceExhausted(sequenceId);
    }

    private void addHumongous(GCLogFile gcLogFile, Long sequenceId, String line) {
        boolean live = line.contains("reclaim candidate 0");
        String size = line
                .replaceFirst(".*object size", "")
                .replaceFirst("start.*", "")
                .replaceFirst("@.*", "")
                .trim();
        if (live) {
            gcLogFile.addLiveHumongous(sequenceId, Long.valueOf(size));
        } else {
            gcLogFile.addDeadHumongous(sequenceId, Long.valueOf(size));
        }
    }

    private void addRegionsSizes(GCLogFile gcLogFile, Long sequenceId, String line) {
        String size = line
                .replaceFirst(".*Used:", "")
                .replaceFirst("K,.*", "")
                .trim();
        String wasted = line
                .replaceFirst(".*Waste:", "")
                .replaceFirst("K.*", "")
                .trim();
        gcLogFile.addRegionSizes(sequenceId, lastRegion, Integer.valueOf(size), Integer.valueOf(wasted));
    }

    private void addRegionsCounts(GCLogFile gcLogFile, Long sequenceId, String line) {
        String regionInfo = line
                .replaceFirst(".* GC", "")
                .replaceFirst(".*\\) ", "")
                .trim();

        String regionName = regionInfo.replaceFirst(":.*", "").trim();
        String before = regionInfo.replaceFirst(".*:", "").replaceFirst("->.*", "").trim();
        String after = regionInfo.replaceFirst(".*->", "").replaceFirst("\\(.*", "").trim();
        String max = regionInfo.replaceFirst(".*->", "");

        Integer maxRegions = null;
        if (max.contains("(")) {
            max = max.replaceFirst(".*\\(", "").replace(")", "").trim();
            if (max.length() > 0) {
                maxRegions = Integer.valueOf(max);
            }
        }
        gcLogFile.addRegionCount(sequenceId, regionName, Integer.valueOf(before), Integer.valueOf(after), maxRegions);

        lastRegion = regionName;
    }

    private String getPhase(String line) {
        String phase = line
                .replaceFirst(".* GC\\(", "")
                .replaceFirst("\\d+\\)\\s*", "")
                .trim();
        return phase;
    }

    private void addAgeCount(GCLogFile gcLogFile, Long sequenceId, String line) {
        String ageStr = line
                .replaceFirst(".* - age", "")
                .replaceFirst(":.*", "")
                .trim();

        String sizeStr;
        if (line.contains("curr")) {
            // Shenandoah format: "- age 1: prev 44147560 bytes, curr 38675848 bytes, mortality 0.12"
            sizeStr = line
                    .replaceFirst(".*curr\\s+", "")
                    .replaceFirst("\\s*bytes.*", "")
                    .trim();
        } else {
            sizeStr = line
                    .replaceFirst(".*:", "")
                    .replaceFirst("bytes.*", "")
                    .trim();
        }

        int age = Integer.parseInt(ageStr);
        long size = Long.parseLong(sizeStr);

        gcLogFile.addAgeWithSize(sequenceId, age, size);
    }
}
