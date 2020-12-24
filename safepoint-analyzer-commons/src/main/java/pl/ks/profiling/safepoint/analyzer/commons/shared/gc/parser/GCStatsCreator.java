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
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.OptionalDouble;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import pl.ks.profiling.safepoint.analyzer.commons.shared.OneFiledAllStats;
import pl.ks.profiling.safepoint.analyzer.commons.shared.OneFiledAllStatsUtil;

public class GCStatsCreator {
    public static GCStats createStats(GCLogFile GCLogFile) {
        GCStats gcStats = new GCStats();
        generateAgingStats(GCLogFile, gcStats);
        generateAgingSummary(GCLogFile, gcStats);

        gcStats.setGcRegions(GCLogFile.getCycleEntries().stream()
                .flatMap(GCLogCycleEntry -> GCLogCycleEntry.getRegionsBeforeGC().keySet().stream())
                .collect(Collectors.toSet()));
        gcStats.setGcPhases(GCLogFile.getCycleEntries().stream()
                .map(GCLogCycleEntry::getPhase)
                .collect(Collectors.toSet()));
        gcStats.setGcAggregatedPhases(GCLogFile.getCycleEntries().stream()
                .map(GCLogCycleEntry::getAggregatedPhase)
                .collect(Collectors.toSet()));
        Map<String, Set<String>> subPhasesMap = new LinkedHashMap<>();
        for (GCLogCycleEntry GCLogCycleEntry : GCLogFile.getCycleEntries()) {
            Set<String> currentParentSet = null;
            for (String phaseName : GCLogCycleEntry.getSubPhasesTime().keySet()) {
                if (phaseName.startsWith("--") && currentParentSet != null) {
                    currentParentSet.add(phaseName);
                } else {
                    currentParentSet = subPhasesMap.computeIfAbsent(phaseName, ignored -> new LinkedHashSet<>());
                }
            }
        }

        Set<String> subPhases = new LinkedHashSet<>();
        for (String parent : subPhasesMap.keySet()) {
            subPhases.add(parent);
            subPhases.addAll(subPhasesMap.get(parent));
        }

        gcStats.setSubPhases(subPhases);

        generatePhaseStats(GCLogFile, gcStats);
        generateAggregatedPhaseStats(GCLogFile, gcStats);
        generateConcurrentCycleStats(GCLogFile, gcStats);
        generateHumongousStats(GCLogFile, gcStats);
        generateToSpaceStats(GCLogFile, gcStats);
        generateFullGcStats(GCLogFile, gcStats);
        return gcStats;
    }

    private static void generateFullGcStats(GCLogFile gcLogFile, GCStats gcStats) {
        gcStats.setFullGcSequenceIds(
                gcLogFile.getCycleEntries().stream()
                        .filter(GCLogCycleEntry -> GCLogCycleEntry.getPhase().contains("Full"))
                        .map(GCLogCycleEntry::getSequenceId)
                        .collect(Collectors.toList())
        );
    }

    private static void generateToSpaceStats(GCLogFile gcLogFile, GCStats gcStats) {
        List<GCToSpaceStats> toSPaceStats = gcLogFile.getCycleEntries().stream()
                .filter(GCLogCycleEntry::isWasToSpaceExhausted)
                .map(GCLogCycleEntry -> {
                    GCToSpaceStats toSpaceStats = new GCToSpaceStats();
                    toSpaceStats.setSequenceId(GCLogCycleEntry.getSequenceId());
                    for (String region : gcStats.getGcRegions()) {
                        String stat = "---";
                        if (GCLogCycleEntry.getRegionsBeforeGC().get(region) != null &&
                                GCLogCycleEntry.getRegionsAfterGC().get(region) != null) {
                            stat = GCLogCycleEntry.getRegionsBeforeGC().get(region) + " --> " + GCLogCycleEntry.getRegionsAfterGC().get(region);
                        }
                        toSpaceStats.getRegionStats().put(region, stat);
                    }
                    return toSpaceStats;
                })
                .collect(Collectors.toList());
        gcStats.setToSpaceStats(toSPaceStats);
    }

    private static void generateHumongousStats(GCLogFile gcLogFile, GCStats gcStats) {
        List<Long> live = gcLogFile.getCycleEntries().stream()
                .flatMap(GCLogCycleEntry -> GCLogCycleEntry.getLiveHumongousSizes().stream())
                .collect(Collectors.toList());
        List<Long> dead = gcLogFile.getCycleEntries().stream()
                .flatMap(GCLogCycleEntry -> GCLogCycleEntry.getDeadHumongousSizes().stream())
                .collect(Collectors.toList());
        List<Long> all = new ArrayList<>(live.size() + dead.size());
        all.addAll(live);
        all.addAll(dead);

        boolean any = false;
        if (live.size() > 0) {
            gcStats.setLiveHumongousStats(OneFiledAllStatsUtil.create(live.stream()
                    .mapToDouble(Long::doubleValue)
                    .toArray()));
            any = true;
        }
        if (dead.size() > 0) {
            gcStats.setDeadHumongousStats(OneFiledAllStatsUtil.create(dead.stream()
                    .mapToDouble(Long::doubleValue)
                    .toArray()));
            any = true;
        }
        if (any) {
            gcStats.setAllHumongousStats(OneFiledAllStatsUtil.create(all.stream()
                    .mapToDouble(Long::doubleValue)
                    .toArray()));
        }
    }

    private static void generateConcurrentCycleStats(GCLogFile gcLogFile, GCStats gcStats) {
        Set<String> phasesName = gcLogFile.getConcurrentCycleEntries().stream()
                .map(GCLogConcurrentCycleEntry::getPhase)
                .collect(Collectors.toSet());
        List<GCConcurrentCycleStats> cyclesStats = phasesName.stream()
                .map(phase -> {
                    GCConcurrentCycleStats stats = new GCConcurrentCycleStats();
                    stats.setName(phase);
                    List<GCLogConcurrentCycleEntry> toProcess = gcLogFile.getConcurrentCycleEntries().stream()
                            .filter(gcConcurrentCycleInfo -> phase.equals(gcConcurrentCycleInfo.getPhase()))
                            .collect(Collectors.toList());
                    stats.setCount((long) toProcess.size());
                    stats.setTime(OneFiledAllStatsUtil.create(toProcess.stream()
                            .mapToDouble(cycle -> cycle.getTime().doubleValue())
                            .toArray()));
                    return stats;
                })
                .collect(Collectors.toList());
        gcStats.setGcConcurrentCycleStats(cyclesStats);
    }

    private static void generatePhaseStats(GCLogFile gcLogFile, GCStats gcStats) {
        List<GCPhaseStats> phasesStats = gcStats.getGcPhases().stream()
                .map(phase -> {
                    GCPhaseStats gcPhaseStats = new GCPhaseStats();
                    gcPhaseStats.setName(phase);
                    List<GCLogCycleEntry> cycles = gcLogFile.getCycleEntries().stream()
                            .filter(GCLogCycleEntry -> phase.equals(GCLogCycleEntry.getPhase()))
                            .collect(Collectors.toList());
                    Map<String, OneFiledAllStats> subPhasesStats = new LinkedHashMap<>();
                    for (String subPhase : gcStats.getSubPhases()) {
                        final String subPhaseName = subPhase;
                        subPhasesStats.put(subPhase, OneFiledAllStatsUtil.create(cycles.stream()
                                .map(GCLogCycleEntry -> GCLogCycleEntry.getSubPhasesTime().get(subPhaseName))
                                .filter(Objects::nonNull)
                                .mapToDouble(BigDecimal::doubleValue)
                                .toArray()
                        ));
                    }
                    gcPhaseStats.setSubPhaseTimes(subPhasesStats);
                    gcPhaseStats.setCount((long) cycles.size());
                    gcPhaseStats.setTime(createAllStats(cycles, cycleInfo -> cycleInfo.getTimeMs().doubleValue()));
                    return gcPhaseStats;
                })
                .sorted(Comparator.comparing(GCPhaseStats::getName))
                .collect(Collectors.toList());
        gcStats.setGcPhaseStats(phasesStats);
    }

    private static void generateAggregatedPhaseStats(GCLogFile gcLogFile, GCStats gcStats) {
        List<GCPhaseStats> phasesStats = gcStats.getGcAggregatedPhases().stream()
                .map(phase -> {
                    GCPhaseStats gcPhaseStats = new GCPhaseStats();
                    gcPhaseStats.setName(phase);
                    List<GCLogCycleEntry> cycles = gcLogFile.getCycleEntries().stream()
                            .filter(GCLogCycleEntry -> phase.equals(GCLogCycleEntry.getAggregatedPhase()))
                            .collect(Collectors.toList());
                    Map<String, OneFiledAllStats> subPhasesStats = new LinkedHashMap<>();
                    for (String subPhase : gcStats.getSubPhases()) {
                        final String subPhaseName = subPhase;
                        subPhasesStats.put(subPhase, OneFiledAllStatsUtil.create(cycles.stream()
                                .map(GCLogCycleEntry -> GCLogCycleEntry.getSubPhasesTime().get(subPhaseName))
                                .filter(Objects::nonNull)
                                .mapToDouble(BigDecimal::doubleValue)
                                .toArray()
                        ));
                    }
                    gcPhaseStats.setSubPhaseTimes(subPhasesStats);
                    gcPhaseStats.setCount((long) cycles.size());
                    gcPhaseStats.setTime(createAllStats(cycles, cycleInfo -> cycleInfo.getTimeMs().doubleValue()));
                    return gcPhaseStats;
                })
                .sorted(Comparator.comparing(GCPhaseStats::getName))
                .collect(Collectors.toList());
        gcStats.setGcAggregatedPhaseStats(phasesStats);
    }

    private static void generateAgingSummary(GCLogFile gcLogFile, GCStats gcStats) {
        if (gcLogFile.getCycleEntries().size() < 2) {
            return;
        }

        GCAgingSummary gcAgingSummary = new GCAgingSummary();
        gcAgingSummary.setSurvivedRatio(new HashMap<>());
        for (int i = 1; i <= gcStats.getMaxSurvivorAge() - 1; i++) {
            int age = i;
            OptionalDouble averageRatio = gcStats.getGcAgingStats().stream()
                    .filter(gcAgingStats -> gcAgingStats.getSurvivedRatio().containsKey(age))
                    .mapToDouble(value -> value.getSurvivedRatio().get(age).doubleValue())
                    .average();
            gcAgingSummary.getSurvivedRatio().put(age, averageRatio.orElse(-1.0));
        }

        for (int i = 1; i <= gcStats.getMaxSurvivorAge(); i++) {
            int age = i;
            double[] sizes = gcLogFile.getCycleEntries().stream()
                    .filter(GCLogCycleEntry -> !GCLogCycleEntry.getBytesInAges().isEmpty())
                    .map(GCLogCycleEntry -> GCLogCycleEntry.getBytesInAges().get(age))
                    .filter(Objects::nonNull)
                    .mapToDouble(Long::doubleValue)
                    .toArray();

            gcAgingSummary.getAgingSizes().put(age, OneFiledAllStatsUtil.create(sizes));
        }
        gcStats.setGcAgingSummary(gcAgingSummary);
    }

    private static void generateAgingStats(GCLogFile gcLogFile, GCStats gcStats) {
        if (gcLogFile.getCycleEntries() == null || gcLogFile.getCycleEntries().size() < 2) {
            return;
        }

        List<GCAgingStats> gcAgingStatsList = new ArrayList<>();
        GCLogCycleEntry prev = null;
        long maxAge = 0L;
        List<GCLogCycleEntry> toProcess = gcLogFile.getCycleEntries().stream()
                .filter(GCLogCycleEntry -> !GCLogCycleEntry.getBytesInAges().isEmpty())
                .collect(Collectors.toList());

        for (GCLogCycleEntry gcLogCycleEntry : toProcess) {
            if (prev != null) {
                GCAgingStats gcAgingStats = new GCAgingStats();
                for (Map.Entry<Integer, Long> agingEntry : gcLogCycleEntry.getBytesInAges().entrySet()) {
                    if (agingEntry.getKey() == 1) {
                        continue;
                    }
                    BigDecimal rate = BigDecimal.valueOf(agingEntry.getValue()).divide(BigDecimal.valueOf(prev.getBytesInAges().get(agingEntry.getKey() - 1)), 3, RoundingMode.HALF_EVEN);
                    gcAgingStats.getSurvivedRatio().put(agingEntry.getKey() - 1, rate);
                }
                gcAgingStats.setSequenceId(gcLogCycleEntry.getSequenceId());
                gcAgingStats.setTimeStamp(gcLogCycleEntry.getTimeStamp());
                gcAgingStatsList.add(gcAgingStats);
            }
            maxAge = Math.max(maxAge, gcLogCycleEntry.getMaxAge());
            prev = gcLogCycleEntry;
        }
        gcStats.setGcAgingStats(gcAgingStatsList);
        gcStats.setMaxSurvivorAge(maxAge);
    }

    private static OneFiledAllStats createAllStats(List<GCLogCycleEntry> cycles, Function<GCLogCycleEntry, Double> valueFunc) {
        double[] values = cycles.stream()
                .map(valueFunc)
                .mapToDouble(Double::doubleValue)
                .toArray();
        return OneFiledAllStatsUtil.create(values);
    }
}
