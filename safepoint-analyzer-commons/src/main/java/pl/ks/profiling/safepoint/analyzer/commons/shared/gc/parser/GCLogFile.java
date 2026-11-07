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

import lombok.Getter;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

public class GCLogFile {
    @Getter
    private List<GCLogCycleEntry> cycleEntries = new ArrayList<>();
    @Getter
    private List<GCLogConcurrentCycleEntry> concurrentCycleEntries = new ArrayList<>();
    @Getter
    private Map<Long, List<String>> rawLogLines = new HashMap<>();
    @Getter
    private GCStats stats;

    private GCLogConcurrentCycleEntry lastConcurrentCycle = null;

    private Map<Long, GCLogCycleEntry> unprocessedCycles = new HashMap<>();

    void newLine(Long cycleId, String line) {
        rawLogLines.computeIfAbsent(cycleId, id -> new ArrayList<>()).add(line);
    }

    void newPhase(Long sequenceId, String phase, BigDecimal timeStamp) {
        GCLogCycleEntry cycle = new GCLogCycleEntry(sequenceId, phase, timeStamp);
        unprocessedCycles.put(sequenceId, cycle);
        if (cycle.isMixed() && lastConcurrentCycle != null) {
            lastConcurrentCycle.nextMixedCollection();
        }
    }

    /**
     * Collections that logged heap sizes; the others would show up as an empty heap in charts.
     */
    public List<GCLogCycleEntry> getCycleEntriesWithHeapSizes() {
        return cycleEntries.stream()
                .filter(GCLogCycleEntry::hasHeapSizes)
                .collect(Collectors.toList());
    }

    boolean isCycleInProgress(Long sequenceId) {
        return unprocessedCycles.containsKey(sequenceId);
    }

    /**
     * Keeps the longest time of a phase logged several times (once per worker).
     */
    void addSubPhaseTimeMax(Long sequenceId, String phase, BigDecimal time) {
        GCLogCycleEntry gcLogCycleEntry = unprocessedCycles.get(sequenceId);
        if (gcLogCycleEntry == null) {
            return;
        }
        BigDecimal current = gcLogCycleEntry.getSubPhasesTime().get(phase);
        if (current == null || current.compareTo(time) < 0) {
            gcLogCycleEntry.addSubPhaseTime(phase, time);
        }
    }

    void addSubPhaseTime(Long sequenceId, String phase, BigDecimal time) {
        GCLogCycleEntry gcLogCycleEntry = unprocessedCycles.get(sequenceId);
        if (gcLogCycleEntry == null) {
            return;
        }
        gcLogCycleEntry.addSubPhaseTime(phase, time);
    }

    void addSizes(Long sequenceId, int heapBeforeGC, int heapAfterGC, int heapSize) {
        GCLogCycleEntry gcLogCycleEntry = unprocessedCycles.get(sequenceId);
        if (gcLogCycleEntry == null) {
            return;
        }
        gcLogCycleEntry.addSizes(heapBeforeGC, heapAfterGC, heapSize);
        if (gcLogCycleEntry.isRemark() && lastConcurrentCycle != null) {
            lastConcurrentCycle.setRemarkReclaimed(heapBeforeGC - heapAfterGC);
        }
    }

    void finishCycle(Long sequenceId) {
        GCLogCycleEntry gcLogCycleEntry = unprocessedCycles.remove(sequenceId);
        if (gcLogCycleEntry == null) {
            return;
        }
        if (gcLogCycleEntry.getTimeMs() == null) {
            BigDecimal total = gcLogCycleEntry.getSubPhasesTime().values().stream()
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            gcLogCycleEntry.addTime(total);
        }
        cycleEntries.add(gcLogCycleEntry);
    }

    void addSizesAndTime(Long sequenceId, int heapBeforeGC, int heapAfterGC, int heapSize, BigDecimal phaseTime) {
        GCLogCycleEntry gcLogCycleEntry = unprocessedCycles.remove(sequenceId);
        if (gcLogCycleEntry == null) {
            return;
        }
        cycleEntries.add(gcLogCycleEntry);
        gcLogCycleEntry.addSizesAndTime(heapBeforeGC, heapAfterGC, heapSize, phaseTime);
        if (gcLogCycleEntry.isRemark() && lastConcurrentCycle != null) {
            lastConcurrentCycle.setRemarkReclaimed(heapBeforeGC - heapAfterGC);
        }
    }

    void addTime(Long sequenceId, BigDecimal phaseTime) {
        GCLogCycleEntry gcLogCycleEntry = unprocessedCycles.get(sequenceId);
        if (gcLogCycleEntry == null) {
            return;
        }
        gcLogCycleEntry.addTime(phaseTime);
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

    /**
     * Called for the line that starts a concurrent cycle. The remark pause and the mixed collections logged after
     * it belong to this cycle.
     */
    void concurrentCycleStarted(Long sequenceId, String phase) {
        GCLogConcurrentCycleEntry cycle = new GCLogConcurrentCycleEntry(sequenceId, phase);
        concurrentCycleEntries.add(cycle);
        if (cycle.isMark()) {
            lastConcurrentCycle = cycle;
        }
    }

    /**
     * Called for the line that ends a concurrent cycle (the one with its duration).
     */
    void concurrentCycleFinished(Long sequenceId, String phase, BigDecimal time) {
        for (int i = concurrentCycleEntries.size() - 1; i >= 0; i--) {
            GCLogConcurrentCycleEntry cycle = concurrentCycleEntries.get(i);
            if (cycle.getSequenceId() == sequenceId && cycle.getPhase().equals(phase) && cycle.getTime() == null) {
                cycle.setTime(time);
                return;
            }
        }
        // The log starts in the middle of the cycle
        concurrentCycleStarted(sequenceId, phase);
        concurrentCycleEntries.get(concurrentCycleEntries.size() - 1).setTime(time);
    }

    void addSurvivorStats(Long sequenceId, long desiredSize, long newThreshold, long maxThreshold) {
        GCLogCycleEntry gcLogCycleEntry = unprocessedCycles.get(sequenceId);
        if (gcLogCycleEntry == null) {
            return;
        }
        gcLogCycleEntry.addSurvivorStats(desiredSize, newThreshold, maxThreshold);
    }

    void parsingCompleted() {
        for (Long sequenceId : new ArrayList<>(unprocessedCycles.keySet())) {
            finishCycle(sequenceId);
        }
        // A concurrent cycle still running when the log ends has no duration
        concurrentCycleEntries.removeIf(cycle -> cycle.getTime() == null);
        // Cycles are finished in the order of their ends; overlapping ZGC cycles end in a different order than
        // they start, and charts expect the order of starts
        cycleEntries.sort(Comparator.comparing(GCLogCycleEntry::getTimeStamp).thenComparing(GCLogCycleEntry::getSequenceId));

        if (stats != null) {
            return;
        }

        stats = GCStatsCreator.createStats(this);
    }
}
