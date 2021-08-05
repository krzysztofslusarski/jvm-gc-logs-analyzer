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

public class ZGCLogFileParser implements FileParser<GCLogFile> {
    private final GCLogFile gcLogFile = new GCLogFile();
    private String lastRegion;

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
            new GcLineParser(includes("gc ", "Concurrent Cycle", "ms"), excludes(), this::addConcurrentCycleDataIfPresent),
            new GcLineParser(includes("gc,phases", "ms"), excludes(), this::addSubPhase),
            new GcLineParser(includes("gc,phases", "ms"), excludes(")  "), this::addPhaseConcurrentSTW),
            new GcLineParser(includes("gc ", "->", "%"), excludes(), this::addSizesBeforeAndAfter),
            new GcLineParser(includes("gc,heap", "Heap after GC invocations"), excludes(), this::addHeapCapacityAfterGC),
            new GcLineParser(includes("regions", "gc,heap", "info"), excludes(), this::addRegionsCounts),
            //new GcLineParser(includes("gc,heap", "trace"), excludes(), this::addRegionsSizes),
            new GcLineParser(includes("gc,humongous", "debug"), excludes(), this::addHumongous),
            new GcLineParser(includes("To-space exhausted"), excludes(), this::toSpaceExhausted)
    );

    private static List<String> includes(String... included) {
        return new ArrayList<>(Arrays.asList(included));
    }

    private static List<String> excludes(String... excluded) {
        return new ArrayList<>(Arrays.asList(excluded));
    }

    public ZGCLogFileParser() {
    }

    @Override
    public void parseLine(String line) {
        try {
            if (isGcLog(line)) {
                useFirstAccepting(line, parsers);
            }
        } catch (Exception e) {
            System.err.println("Error on line '" + line + "'");
            //e.printStackTrace();
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
        gcLogFile.newPhase(sequenceId, getPhase(line), ParserUtils.getTimeStamp(line), true);
    }

    private void addConcurrentCycleDataIfPresent(GCLogFile gcLogFile, Long sequenceId, String line) {
        Pattern pattern = Pattern.compile("\\d+.\\d+ms");
        Matcher matcher = pattern.matcher(line);
        matcher.find();
        String time = matcher.group().replace("ms", "").replace(",", ".");
        gcLogFile.newConcurrentCycle(sequenceId, new BigDecimal(time));
    }

    @Override
    public GCLogFile fetchData() {
        gcLogFile.parsingCompleted();
        return gcLogFile;
    }

    private void addPhaseConcurrentSTW(GCLogFile gcLogFile, Long sequenceId, String line) {
        String phaseWithTime = line.replaceFirst(".*GC\\(\\d+\\)", "").trim();
        int indexOfSpace = phaseWithTime.lastIndexOf(" ");
        String phase = phaseWithTime.substring(0, indexOfSpace);
        String time = phaseWithTime.substring(indexOfSpace + 1).replaceAll("ms", "").replaceAll(",", ".");
        gcLogFile.addSubPhaseTime(sequenceId, phase, new BigDecimal(time));
    }

    private void addSubPhase(GCLogFile gcLogFile, Long sequenceId, String line) {
        Pattern pattern = Pattern.compile("GC\\(.+\\) (.+) ([^ ]+)ms");
        Matcher matcher = pattern.matcher(line);
        matcher.find();
        String phase = matcher.group(1);
        String time = matcher.group(2);
        //System.out.println("Recognized subphase: " + phase + " " + time + "ms");
        gcLogFile.addSubPhaseTime(sequenceId, phase, new BigDecimal(time));
    }

    private void addSizesBeforeAndAfter(GCLogFile gcLogFile, Long sequenceId, String line) {
        Pattern pattern = Pattern.compile("\\d+M");
        Matcher matcher = pattern.matcher(line);
        matcher.find();
        String before = matcher.group().replace("M", "");
        matcher.find();
        String after = matcher.group().replace("M", "");

        gcLogFile.addHeapBeforeAndAfterGCAndSumUpPhase(sequenceId, Integer.parseInt(before), Integer.parseInt(after));
    }

    private void addHeapCapacityAfterGC(GCLogFile gcLogFile, Long sequenceId, String line) {
        Pattern pattern = Pattern.compile(", capacity (\\d+)M");
        Matcher matcher = pattern.matcher(line);
        matcher.find();
        String capacity = matcher.group(1);

        //System.out.println("Found heap cap: " + capacity);
        gcLogFile.addHeapCapacityAfterGC(sequenceId, Integer.parseInt(capacity));
    }

    private void toSpaceExhausted(GCLogFile gcLogFile, Long sequenceId, String line) {
        gcLogFile.toSpaceExhausted(sequenceId);
    }

    private void addHumongous(GCLogFile gcLogFile, Long sequenceId, String line) {
        boolean live = line.contains("Live");
        String size = line
                .replaceFirst(".*object size", "")
                .replaceFirst("start.*", "")
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
                .replaceFirst(".*\\) ", "")
                .trim();
        return phase;
    }
}