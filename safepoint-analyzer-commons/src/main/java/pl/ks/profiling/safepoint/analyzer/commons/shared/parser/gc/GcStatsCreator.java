package pl.ks.profiling.safepoint.analyzer.commons.shared.parser.gc;

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

public class GcStatsCreator {
    public static GcStats createStats(GcLogFile gcLogFile) {
        GcStats gcStats = new GcStats();
        generateAgingStats(gcLogFile, gcStats);
        generateAgingSummary(gcLogFile, gcStats);

        gcStats.setGcRegions(gcLogFile.getGcCycleInfos().stream()
                .flatMap(gcCycleInfo -> gcCycleInfo.getRegionsBeforeGC().keySet().stream())
                .collect(Collectors.toSet()));
        gcStats.setGcPhases(gcLogFile.getGcCycleInfos().stream()
                .map(GcCycleInfo::getPhase)
                .collect(Collectors.toSet()));
        gcStats.setGcAggregatedPhases(gcLogFile.getGcCycleInfos().stream()
                .map(GcCycleInfo::getAggregatedPhase)
                .collect(Collectors.toSet()));
        Map<String, Set<String>> subPhasesMap = new LinkedHashMap<>();
        for (GcCycleInfo gcCycleInfo : gcLogFile.getGcCycleInfos()) {
            Set<String> currentParentSet = null;
            for (String phaseName : gcCycleInfo.getSubPhasesTime().keySet()) {
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

        generatePhaseStats(gcLogFile, gcStats);
        generateAggregatedPhaseStats(gcLogFile, gcStats);
        generateConcurrentCycleStats(gcLogFile, gcStats);
        generateHumongousStats(gcLogFile, gcStats);
        generateToSpaceStats(gcLogFile, gcStats);
        generateFullGcStats(gcLogFile, gcStats);
        return gcStats;
    }

    private static void generateFullGcStats(GcLogFile gcLogFile, GcStats gcStats) {
        gcStats.setFullGcSequenceIds(
                gcLogFile.getGcCycleInfos().stream()
                        .filter(gcCycleInfo -> gcCycleInfo.getPhase().contains("Full"))
                        .map(GcCycleInfo::getSequenceId)
                        .collect(Collectors.toList())
        );
    }

    private static void generateToSpaceStats(GcLogFile gcLogFile, GcStats gcStats) {
        List<GcToSpaceStats> toSPaceStats = gcLogFile.getGcCycleInfos().stream()
                .filter(GcCycleInfo::isWasToSpaceExhausted)
                .map(gcCycleInfo -> {
                    GcToSpaceStats toSpaceStats = new GcToSpaceStats();
                    toSpaceStats.setSequenceId(gcCycleInfo.getSequenceId());
                    for (String region : gcStats.getGcRegions()) {
                        String stat = "---";
                        if (gcCycleInfo.getRegionsBeforeGC().get(region) != null &&
                                gcCycleInfo.getRegionsAfterGC().get(region) != null) {
                            stat = gcCycleInfo.getRegionsBeforeGC().get(region) + " --> " + gcCycleInfo.getRegionsAfterGC().get(region);
                        }
                        toSpaceStats.getRegionStats().put(region, stat);
                    }
                    return toSpaceStats;
                })
                .collect(Collectors.toList());
        gcStats.setToSpaceStats(toSPaceStats);
    }

    private static void generateHumongousStats(GcLogFile gcLogFile, GcStats gcStats) {
        List<Long> live = gcLogFile.getGcCycleInfos().stream()
                .flatMap(gcCycleInfo -> gcCycleInfo.getLiveHumongousSizes().stream())
                .collect(Collectors.toList());
        List<Long> dead = gcLogFile.getGcCycleInfos().stream()
                .flatMap(gcCycleInfo -> gcCycleInfo.getDeadHumongousSizes().stream())
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

    private static void generateConcurrentCycleStats(GcLogFile gcLogFile, GcStats gcStats) {
        Set<String> phasesName = gcLogFile.getGcConcurrentCycleInfos().stream()
                .map(GcConcurrentCycleInfo::getPhase)
                .collect(Collectors.toSet());
        List<GcConcurrentCycleStats> cyclesStats = phasesName.stream()
                .map(phase -> {
                    GcConcurrentCycleStats stats = new GcConcurrentCycleStats();
                    stats.setName(phase);
                    List<GcConcurrentCycleInfo> toProcess = gcLogFile.getGcConcurrentCycleInfos().stream()
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

    private static void generatePhaseStats(GcLogFile gcLogFile, GcStats gcStats) {
        List<GcPhaseStats> phasesStats = gcStats.getGcPhases().stream()
                .map(phase -> {
                    GcPhaseStats gcPhaseStats = new GcPhaseStats();
                    gcPhaseStats.setName(phase);
                    List<GcCycleInfo> cycles = gcLogFile.getGcCycleInfos().stream()
                            .filter(gcCycleInfo -> phase.equals(gcCycleInfo.getPhase()))
                            .collect(Collectors.toList());
                    Map<String, OneFiledAllStats> subPhasesStats = new LinkedHashMap<>();
                    for (String subPhase : gcStats.getSubPhases()) {
                        final String subPhaseName = subPhase;
                        subPhasesStats.put(subPhase, OneFiledAllStatsUtil.create(cycles.stream()
                                .map(gcCycleInfo -> gcCycleInfo.getSubPhasesTime().get(subPhaseName))
                                .filter(Objects::nonNull)
                                .mapToDouble(BigDecimal::doubleValue)
                                .toArray()
                        ));
                    }
                    gcPhaseStats.setSubPhaseTimes(subPhasesStats);
                    gcPhaseStats.setCount((long) cycles.size());
                    gcPhaseStats.setTime(createAllStats(cycles, cycleInfo -> cycleInfo.getTime().doubleValue()));
                    return gcPhaseStats;
                })
                .sorted(Comparator.comparing(GcPhaseStats::getName))
                .collect(Collectors.toList());
        gcStats.setGcPhaseStats(phasesStats);
    }

    private static void generateAggregatedPhaseStats(GcLogFile gcLogFile, GcStats gcStats) {
        List<GcPhaseStats> phasesStats = gcStats.getGcAggregatedPhases().stream()
                .map(phase -> {
                    GcPhaseStats gcPhaseStats = new GcPhaseStats();
                    gcPhaseStats.setName(phase);
                    List<GcCycleInfo> cycles = gcLogFile.getGcCycleInfos().stream()
                            .filter(gcCycleInfo -> phase.equals(gcCycleInfo.getAggregatedPhase()))
                            .collect(Collectors.toList());
                    Map<String, OneFiledAllStats> subPhasesStats = new LinkedHashMap<>();
                    for (String subPhase : gcStats.getSubPhases()) {
                        final String subPhaseName = subPhase;
                        subPhasesStats.put(subPhase, OneFiledAllStatsUtil.create(cycles.stream()
                                .map(gcCycleInfo -> gcCycleInfo.getSubPhasesTime().get(subPhaseName))
                                .filter(Objects::nonNull)
                                .mapToDouble(BigDecimal::doubleValue)
                                .toArray()
                        ));
                    }
                    gcPhaseStats.setSubPhaseTimes(subPhasesStats);
                    gcPhaseStats.setCount((long) cycles.size());
                    gcPhaseStats.setTime(createAllStats(cycles, cycleInfo -> cycleInfo.getTime().doubleValue()));
                    return gcPhaseStats;
                })
                .sorted(Comparator.comparing(GcPhaseStats::getName))
                .collect(Collectors.toList());
        gcStats.setGcAggregatedPhaseStats(phasesStats);
    }

    private static void generateAgingSummary(GcLogFile gcLogFile, GcStats gcStats) {
        if (gcLogFile.getGcCycleInfos().size() < 2) {
            return;
        }

        GcAgingSummary gcAgingSummary = new GcAgingSummary();
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
            double[] sizes = gcLogFile.getGcCycleInfos().stream()
                    .filter(gcCycleInfo -> !gcCycleInfo.getBytesInAges().isEmpty())
                    .map(gcCycleInfo -> gcCycleInfo.getBytesInAges().get(age))
                    .filter(Objects::nonNull)
                    .mapToDouble(Long::doubleValue)
                    .toArray();

            gcAgingSummary.getAgingSizes().put(age, OneFiledAllStatsUtil.create(sizes));
        }
        gcStats.setGcAgingSummary(gcAgingSummary);
    }

    private static void generateAgingStats(GcLogFile gcLogFile, GcStats gcStats) {
        if (gcLogFile.getGcCycleInfos() == null || gcLogFile.getGcCycleInfos().size() < 2) {
            return;
        }

        List<GcAgingStats> gcAgingStatsList = new ArrayList<>();
        GcCycleInfo prev = null;
        long maxAge = 0L;
        List<GcCycleInfo> toProcess = gcLogFile.getGcCycleInfos().stream()
                .filter(gcCycleInfo -> !gcCycleInfo.getBytesInAges().isEmpty())
                .collect(Collectors.toList());

        for (GcCycleInfo gcCycleInfo : toProcess) {
            if (prev != null) {
                GcAgingStats gcAgingStats = new GcAgingStats();
                for (Map.Entry<Integer, Long> agingEntry : gcCycleInfo.getBytesInAges().entrySet()) {
                    if (agingEntry.getKey() == 1) {
                        continue;
                    }
                    BigDecimal rate = BigDecimal.valueOf(agingEntry.getValue()).divide(BigDecimal.valueOf(prev.getBytesInAges().get(agingEntry.getKey() - 1)), 3, RoundingMode.HALF_EVEN);
                    gcAgingStats.getSurvivedRatio().put(agingEntry.getKey() - 1, rate);
                }
                gcAgingStats.setSequenceId(gcCycleInfo.getSequenceId());
                gcAgingStats.setTimeStamp(gcCycleInfo.getTimeStamp());
                gcAgingStatsList.add(gcAgingStats);
            }
            maxAge = Math.max(maxAge, gcCycleInfo.getMaxAge());
            prev = gcCycleInfo;
        }
        gcStats.setGcAgingStats(gcAgingStatsList);
        gcStats.setMaxSurvivorAge(maxAge);
    }

    private static OneFiledAllStats createAllStats(List<GcCycleInfo> cycles, Function<GcCycleInfo, Double> valueFunc) {
        double[] values = cycles.stream()
                .map(valueFunc)
                .mapToDouble(Double::doubleValue)
                .toArray();
        return OneFiledAllStatsUtil.create(values);
    }
}
