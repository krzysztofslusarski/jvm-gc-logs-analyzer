package pl.ks.profiling.safepoint.analyzer.commons.shared.parser.gc;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.Getter;

public class GCLogFile {
    @Getter
    private List<GCLogCycleEntry> cycleEntries = new ArrayList<>();
    @Getter
    private List<GCLogConcurrentCycleEntry> concurrentCycleEntries = new ArrayList<>();
    @Getter
    private Map<Long, List<String>> rawLogLines = new HashMap<>();
    @Getter
    private GCStats stats;

    private Map<Long, GCLogCycleEntry> unprocessedCycles = new HashMap<>();


    void newLine(Long cycleId, String line) {
        rawLogLines.computeIfAbsent(cycleId, id -> new ArrayList<>()).add(line);
    }

    void newPhase(Long sequenceId, String phase, BigDecimal timeStamp) {
        unprocessedCycles.put(sequenceId, new GCLogCycleEntry(sequenceId, phase, timeStamp));
    }

    void addSubPhaseTime(Long sequenceId, String phase, BigDecimal time) {
        GCLogCycleEntry gcLogCycleEntry = unprocessedCycles.get(sequenceId);
        if (gcLogCycleEntry == null) {
            return;
        }
        gcLogCycleEntry.addSubPhaseTime(phase, time);
    }

    void addSizesAndTime(Long sequenceId, int heapBeforeGC, int heapAfterGC, int heapSize, BigDecimal phaseTime) {
        GCLogCycleEntry gcLogCycleEntry = unprocessedCycles.remove(sequenceId);
        if (gcLogCycleEntry == null) {
            return;
        }
        cycleEntries.add(gcLogCycleEntry);
        gcLogCycleEntry.addSizesAndTime(heapBeforeGC, heapAfterGC, heapSize, phaseTime);
    }

    void addRegionCount(Long sequenceId, String regionName, Integer regionsBeforeGC, Integer regionsAfterGC, Integer maxRegions) {
        GCLogCycleEntry gcLogCycleEntry = unprocessedCycles.get(sequenceId);
        if (gcLogCycleEntry == null) {
            return;
        }
        gcLogCycleEntry.addRegionCount(regionName, regionsBeforeGC, regionsAfterGC, maxRegions);
    }

    void addRegionSizes(Long sequenceId, String regionName, Integer size, Integer wasted) {
        GCLogCycleEntry gcLogCycleEntry = unprocessedCycles.get(sequenceId);
        if (gcLogCycleEntry == null) {
            return;
        }
        gcLogCycleEntry.addRegionSizes(regionName, size, wasted);
    }

    void addLiveHumongous(Long sequenceId, Long size) {
        GCLogCycleEntry gcLogCycleEntry = unprocessedCycles.get(sequenceId);
        if (gcLogCycleEntry == null) {
            return;
        }
        gcLogCycleEntry.addLiveHumongous(size);
    }

    void addDeadHumongous(Long sequenceId, Long size) {
        GCLogCycleEntry gcLogCycleEntry = unprocessedCycles.get(sequenceId);
        if (gcLogCycleEntry == null) {
            return;
        }
        gcLogCycleEntry.addDeadHumongous(size);
    }

    void addAgeWithSize(Long sequenceId, int age, long size) {
        GCLogCycleEntry gcLogCycleEntry = unprocessedCycles.get(sequenceId);
        if (gcLogCycleEntry == null) {
            return;
        }
        gcLogCycleEntry.addAgeWithSize(age, size);
    }

    void toSpaceExhausted(Long sequenceId) {
        GCLogCycleEntry gcLogCycleEntry = unprocessedCycles.get(sequenceId);
        if (gcLogCycleEntry == null) {
            return;
        }
        gcLogCycleEntry.toSpaceExhausted();
    }

    void newConcurrentCycle(Long sequenceId, BigDecimal time) {
        concurrentCycleEntries.add(new GCLogConcurrentCycleEntry(sequenceId, time));
    }

    void parsingCompleted() {
        if (stats != null) {
            return;
        }

        stats = GCStatsCreator.createStats(this);
    }
}
