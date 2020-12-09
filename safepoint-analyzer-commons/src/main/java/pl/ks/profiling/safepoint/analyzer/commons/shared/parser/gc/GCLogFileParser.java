package pl.ks.profiling.safepoint.analyzer.commons.shared.parser.gc;

import java.math.BigDecimal;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import pl.ks.profiling.safepoint.analyzer.commons.FileParser;

public class GCLogFileParser implements FileParser<GCLogFile> {
    private GCLogFile gcLogFile = new GCLogFile();
    private String lastRegion;

    public GCLogFileParser() {
    }

    @Override
    public void parseLine(String line) {
        Long sequenceId = null;
        if (line.contains("gc") && line.contains("GC(")) {
            sequenceId = getSequenceId(line);
        } else {
            return;
        }

        gcLogFile.newLine(sequenceId, line);
        if (line.contains("gc,start")) {
            gcLogFile.newPhase(sequenceId, getPhase(line), getTimeStamp(line));
        } else if (line.contains("gc ") && line.contains("Concurrent Cycle") && line.contains("ms")) {
            addConcurrentCycleDataIfPresent(line);
        } else if (line.contains("gc,phases") && line.contains("ms") && line.contains(")   ") && !line.contains(")       ") &&
                !line.contains("Queue Fixup") && !line.contains("Table Fixup")) {
            addPhaseYoungAndMixed(sequenceId, line);
        } else if (line.contains("gc,phases") && line.contains("ms") && !line.contains(")  ")) {
            addPhaseConcurrentSTW(sequenceId, line);
        } else if (line.contains("gc ") && line.contains("->")) {
            addSizesAndTime(sequenceId, line);
        } else if (line.contains("regions") && line.contains("gc,heap") && line.contains("info")) {
            lastRegion = addRegionsCounts(sequenceId, line, gcLogFile);
        } else if (line.contains("gc,heap") && line.contains("trace")) {
            addRegionsSizes(sequenceId, line, lastRegion);
        } else if (line.contains("gc,humongous") && line.contains("debug")) {
            addHumongous(sequenceId, line);
        } else if (line.contains("- age")) {
            addAgeCount(sequenceId, line);
        } else if (line.contains("To-space exhausted")) {
            gcLogFile.toSpaceExhausted(sequenceId);
        }
    }

    private void addConcurrentCycleDataIfPresent(String line) {
        Pattern pattern = Pattern.compile("\\d+.\\d+ms");
        Matcher matcher = pattern.matcher(line);
        matcher.find();
        String time = matcher.group().replace("ms", "").replace(",", ".");
        gcLogFile.newConcurrentCycle(getSequenceId(line), new BigDecimal(time));
    }

    @Override
    public GCLogFile fetchData() {
        gcLogFile.parsingCompleted();
        return gcLogFile;
    }

    private Long getSequenceId(String line) {
        Pattern pattern = Pattern.compile("\\(\\d+\\)");
        Matcher matcher = pattern.matcher(line);
        matcher.find();
        return Long.valueOf(matcher.group().replace("(", "").replace(")", "").trim());
    }

    private BigDecimal getTimeStamp(String line) {
        Pattern pattern = Pattern.compile("\\[\\d+.\\d+s]");
        Matcher matcher = pattern.matcher(line);
        matcher.find();
        return new BigDecimal(matcher.group().replace(',', '.').replace("[", "").replace("s]", "").trim());
    }

    private void addPhaseConcurrentSTW(Long sequenceId, String line) {
        String phaseWithTime = line.replaceFirst(".*GC\\(\\d+\\)", "").trim();
        int indexOfSpace = phaseWithTime.lastIndexOf(" ");
        String phase = phaseWithTime.substring(0, indexOfSpace);
        String time = phaseWithTime.substring(indexOfSpace + 1).replaceAll("ms", "");
        gcLogFile.addSubPhaseTime(sequenceId, phase, new BigDecimal(time));
    }

    private void addPhaseYoungAndMixed(Long sequenceId, String line) {
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

    private void addSizesAndTime(Long sequenceId, String line) {
        Pattern pattern = Pattern.compile("\\d+M");
        Matcher matcher = pattern.matcher(line);
        matcher.find();
        String before = matcher.group().replace("M", "");
        matcher.find();
        String after = matcher.group().replace("M", "");
        matcher.find();
        String heapSize = matcher.group().replace("M", "");
        pattern = Pattern.compile("\\d+.\\d+ms");
        matcher = pattern.matcher(line);
        matcher.find();
        String time = matcher.group().replace("ms", "").replace(",", ".");

        gcLogFile.addSizesAndTime(sequenceId, Integer.parseInt(before), Integer.parseInt(after), Integer.parseInt(heapSize), new BigDecimal(time));
    }

    private void addHumongous(Long sequenceId, String line) {
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

    private void addRegionsSizes(Long sequenceId, String line, String regionName) {
        String size = line
                .replaceFirst(".*Used:", "")
                .replaceFirst("K,.*", "")
                .trim();
        String wasted = line
                .replaceFirst(".*Waste:", "")
                .replaceFirst("K.*", "")
                .trim();
        gcLogFile.addRegionSizes(sequenceId, regionName, Integer.valueOf(size), Integer.valueOf(wasted));
    }

    private String addRegionsCounts(Long sequenceId, String line, GCLogFile gcLogFile) {
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

        return regionName;
    }

    private String getPhase(String line) {
        String phase = line
                .replaceFirst(".* GC\\(", "")
                .replaceFirst(".*\\) Pause", "Pause")
                .trim();
        return phase;
    }

    private void addAgeCount(Long sequenceId, String line) {
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

        gcLogFile.addAgeWithSize(sequenceId, age, size);
    }
}
