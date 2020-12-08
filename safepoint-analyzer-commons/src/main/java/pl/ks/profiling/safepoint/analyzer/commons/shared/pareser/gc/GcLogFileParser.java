package pl.ks.profiling.safepoint.analyzer.commons.shared.pareser.gc;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import pl.ks.profiling.safepoint.analyzer.commons.FileParser;

public class GcLogFileParser implements FileParser<GcLogFile> {
    private GcLogFile gcLogFile;
    private List<GcCycleInfo> gcCycleInfos = new ArrayList<>();
    private List<GcConcurrentCycleInfo> gcConcurrentCycleInfos = new ArrayList<>();
    private Map<Long, GcCycleInfo> unprocessedCycles = new LinkedHashMap<>();
    LinkedHashMap<Long, List<String>> lines = new LinkedHashMap<>();
    private String lastRegion;

    public GcLogFileParser() {
        gcLogFile = new GcLogFile();
        gcLogFile.setGcCycleInfos(gcCycleInfos);
        gcLogFile.setGcConcurrentCycleInfos(gcConcurrentCycleInfos);
        gcLogFile.setLines(lines);
    }

    @Override
    public void parseLine(String line) {
        Long sequenceId = null;
        if (line.contains("gc") && line.contains("GC(")) {
            sequenceId = getSequenceId(line);
        }
        lines.computeIfAbsent(sequenceId, id -> new ArrayList<>()).add(line);

        if (line.contains("gc,start")) {
            GcCycleInfo current = new GcCycleInfo();
            current.setSequenceId(sequenceId);
            current.setPhase(getPhase(line));
            current.setTimeStamp(getTimeStamp(line));
            setAggregatedPhase(current);
            unprocessedCycles.put(current.getSequenceId(), current);
        } else if (line.contains("gc ") && line.contains("Concurrent Cycle") && line.contains("ms")) {
            addConcurrentCycleDataIfPresent(line);
        } else if (line.contains("gc,phases") && line.contains("ms") && line.contains(")   ") && !line.contains(")       ") &&
                !line.contains("Queue Fixup") && !line.contains("Table Fixup")) {
            GcCycleInfo current = unprocessedCycles.get(sequenceId);
            if (current != null) {
                parsePhaseYoungAndMixed(line, current);
            }
        } else if (line.contains("gc,phases") && line.contains("ms") && !line.contains(")  ")) {
            GcCycleInfo current = unprocessedCycles.get(sequenceId);
            if (current != null) {
                parsePhaseConcurrent(line, current);
            }
        } else if (line.contains("gc ") && line.contains("->")) {
            GcCycleInfo current = unprocessedCycles.remove(sequenceId);
            if (current != null) {
                gcCycleInfos.add(current);
                parseSizesAndTime(line, current);
            }
        } else if (line.contains("regions") && line.contains("gc,heap") && line.contains("info")) {
            GcCycleInfo current = unprocessedCycles.get(sequenceId);
            if (current != null) {
                lastRegion = parseRegionsCounts(line, current);
            }
        } else if (line.contains("gc,heap") && line.contains("trace")) {
            GcCycleInfo current = unprocessedCycles.get(sequenceId);
            if (current != null && lastRegion != null) {
                parseRegionsSizes(line, current, lastRegion);
            }
        } else if (line.contains("gc,humongous") && line.contains("debug")) {
            GcCycleInfo current = unprocessedCycles.get(sequenceId);
            if (current != null) {
                parseHumongous(line, current);
            }
        } else if (line.contains("- age")) {
            GcCycleInfo current = unprocessedCycles.get(sequenceId);
            if (current != null) {
                parseAgeCount(line, current);
            }
        } else if (line.contains("To-space exhausted")) {
            GcCycleInfo current = unprocessedCycles.get(sequenceId);
            if (current != null) {
                current.setWasToSpaceExhausted(true);
            }
        }
    }

    private void addConcurrentCycleDataIfPresent(String line) {
        Pattern pattern = Pattern.compile("\\d+.\\d+ms");
        Matcher matcher = pattern.matcher(line);
        matcher.find();
        String time = matcher.group().replace("ms", "").replace(",", ".");
        GcConcurrentCycleInfo gcConcurrentCycleInfo = new GcConcurrentCycleInfo();
        gcConcurrentCycleInfo.setPhase("Concurrent Cycle");
        gcConcurrentCycleInfo.setSequenceId(getSequenceId(line));
        gcConcurrentCycleInfo.setTime(new BigDecimal(time));
        gcConcurrentCycleInfos.add(gcConcurrentCycleInfo);
    }

    @Override
    public GcLogFile fetchData() {
        return gcLogFile;
    }

    private static Long getSequenceId(String line) {
        Pattern pattern = Pattern.compile("\\(\\d+\\)");
        Matcher matcher = pattern.matcher(line);
        matcher.find();
        return Long.valueOf(matcher.group().replace("(", "").replace(")", "").trim());
    }

    private static BigDecimal getTimeStamp(String line) {
        Pattern pattern = Pattern.compile("\\[\\d+.\\d+s]");
        Matcher matcher = pattern.matcher(line);
        matcher.find();
        return new BigDecimal(matcher.group().replace(',', '.').replace("[", "").replace("s]", "").trim());
    }

    private static void parsePhaseConcurrent(String line, GcCycleInfo current) {
        String phaseWithTime = line.replaceFirst(".*GC\\(\\d+\\)", "").trim();
        int indexOfSpace = phaseWithTime.lastIndexOf(" ");
        String phase = phaseWithTime.substring(0, indexOfSpace);
        String time = phaseWithTime.substring(indexOfSpace + 1).replaceAll("ms", "");
        current.getSubPhasesTime().put(phase, new BigDecimal(time));
    }

    private static void parsePhaseYoungAndMixed(String line, GcCycleInfo current) {
        String phase = line.replaceFirst(".*GC\\(\\d+\\)", "").replaceFirst(":.*", "").replace("   ", "").replaceAll("  ", "|______").replace(" (ms)", "");
        if (line.contains("Max:")) {
            String time = line.replaceFirst(".*Max:", "").replaceFirst(",.*", "").trim().replace(',', '.');
            current.getSubPhasesTime().put(phase, new BigDecimal(time));
        } else if (line.contains("skipped")) {
            current.getSubPhasesTime().put(phase, BigDecimal.ZERO);
        } else {
            String time = line.replaceFirst(".*:", "").replaceFirst("ms", "").trim().replace(',', '.');
            current.getSubPhasesTime().put(phase, new BigDecimal(time));
        }
    }

    private static void parseSizesAndTime(String line, GcCycleInfo current) {
        Pattern pattern = Pattern.compile("\\d+M");
        Matcher matcher = pattern.matcher(line);
        matcher.find();
        String before = matcher.group().replace("M", "");
        matcher.find();
        String after = matcher.group().replace("M", "");
        matcher.find();
        String heapSize = matcher.group().replace("M", "");
        current.setHeapBeforeGC(Integer.parseInt(before));
        current.setHeapAfterGC(Integer.parseInt(after));
        current.setHeapSize(Integer.parseInt(heapSize));

        pattern = Pattern.compile("\\d+.\\d+ms");
        matcher = pattern.matcher(line);
        matcher.find();
        String time = matcher.group().replace("ms", "").replace(",", ".");
        current.setTime(new BigDecimal(time));
    }

    private static void parseHumongous(String line, GcCycleInfo current) {
        boolean live = line.contains("Live");
        String size = line
                .replaceFirst(".*object size", "")
                .replaceFirst("start.*", "")
                .trim();
        if (live) {
            current.getLiveHumongousSizes().add(Long.valueOf(size));
        } else {
            current.getDeadHumongousSizes().add(Long.valueOf(size));
        }
    }

    private static void parseRegionsSizes(String line, GcCycleInfo current, String regionName) {
        String size = line
                .replaceFirst(".*Used:", "")
                .replaceFirst("K,.*", "")
                .trim();
        String wasted = line
                .replaceFirst(".*Waste:", "")
                .replaceFirst("K.*", "")
                .trim();
        current.getRegionsSizeAfterGC().put(regionName, Integer.valueOf(size));
        current.getRegionsWastedAfterGC().put(regionName, Integer.valueOf(wasted));
    }

    private static String parseRegionsCounts(String line, GcCycleInfo current) {
        String regionInfo = line
                .replaceFirst(".* GC", "")
                .replaceFirst(".*\\) ", "")
                .trim();

        String regionName = regionInfo.replaceFirst(":.*", "").trim();
        String before = regionInfo.replaceFirst(".*:", "").replaceFirst("->.*", "").trim();
        String after = regionInfo.replaceFirst(".*->", "").replaceFirst("\\(.*", "").trim();
        String max = regionInfo.replaceFirst(".*->", "");
        current.getRegionsAfterGC().put(regionName, Integer.valueOf(after));
        current.getRegionsBeforeGC().put(regionName, Integer.valueOf(before));
        if (max.contains("(")) {
            max = max.replaceFirst(".*\\(", "").replace(")", "").trim();
            if (max.length() > 0) {
                current.getRegionsMax().put(regionName, Integer.valueOf(max));
            }
        }
        return regionName;
    }

    private static String getPhase(String line) {
        String phase = line
                .replaceFirst(".* GC\\(", "")
                .replaceFirst(".*\\) Pause", "Pause")
                .trim();
        return phase;
    }

    private static void setAggregatedPhase(GcCycleInfo current) {
        String phase = current.getPhase();
        if (phase.contains("Pause Young")) {
            if (phase.contains("(Mixed)")) {
                current.setAggregatedPhase("Mixed collection");
                current.setGenuineCollection(true);
            } else if (phase.contains("(Allocation Failure)")) {
                current.setAggregatedPhase("Young collection");
                current.setGenuineCollection(true);
            } else if (phase.contains("(Normal)")) {
                current.setAggregatedPhase("Young collection");
                current.setGenuineCollection(true);
            } else {
                current.setAggregatedPhase("Young collection - piggybacks");
            }
        } else if (phase.contains("Pause Full")) {
            current.setAggregatedPhase("Full collection");
            current.setGenuineCollection(true);
        } else {
            current.setAggregatedPhase(phase);
        }
    }

    private static void parseAgeCount(String line, GcCycleInfo current) {
        String ageStr = line
                .replaceFirst(".* - age", "")
                .replaceFirst(":.*", "")
                .trim();
        String sizeStr = line
                .replaceFirst(".*:", "")
                .replaceFirst("bytes.*", "")
                .trim();

        int age = Integer.parseInt(ageStr);
        long size = Long.parseLong(sizeStr);

        current.getBytesInAges().put(age, size);
        current.setMaxAge(Math.max(current.getMaxAge(), age));
    }
}
