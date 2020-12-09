package pl.ks.profiling.safepoint.analyzer.commons.shared.gc.page;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import pl.ks.profiling.gui.commons.Chart;
import pl.ks.profiling.gui.commons.Page;
import pl.ks.profiling.safepoint.analyzer.commons.shared.JvmLogFile;
import pl.ks.profiling.safepoint.analyzer.commons.shared.PageCreator;
import pl.ks.profiling.safepoint.analyzer.commons.shared.gc.parser.GCLogCycleEntry;

@RequiredArgsConstructor
public class GCAllocationRateInTime implements PageCreator {
    private final BigDecimal minuteCount;

    @Override
    public Page create(JvmLogFile jvmLogFile, DecimalFormat decimalFormat) {
        if (jvmLogFile.getGcLogFile().getStats().getGcRegions().isEmpty()) {
            return null;
        }
        Map<Long, List<BigDecimal>> byTimeMap = createByTimeMap(jvmLogFile);
        return Page.builder()
                .menuName("Allocation rate (in time) - " + minuteCount + "m")
                .fullName("Allocation rate (in time) - " + minuteCount + " minutes period")
                .info("Charts presents allocation rate in MB/s.")
                .icon(Page.Icon.CHART)
                .pageContents(List.of(
                        Chart.builder()
                                .title("Avg. of avg. allocation rate in time period")
                                .chartType(Chart.ChartType.POINTS)
                                .data(getChartAvg(byTimeMap))
                                .build(),
                        Chart.builder()
                                .title("Max. allocation rate in time period")
                                .chartType(Chart.ChartType.POINTS)
                                .data(getChartMax(byTimeMap))
                                .build()

                        )
                )
                .build();
    }

    private static Object[][] getChartMax(Map<Long, List<BigDecimal>> byTimeMap) {
        if (byTimeMap == null) return null;

        Object[][] stats = new Object[byTimeMap.size() + 1][2];
        stats[0][0] = "Time";
        stats[0][1] = "Max";

        int j = 1;
        for (Map.Entry<Long, List<BigDecimal>> cycle : byTimeMap.entrySet()) {
            stats[j][0] = cycle.getKey();
            BigDecimal rate = cycle.getValue().stream()
                    .reduce(BigDecimal::max)
                    .orElse(BigDecimal.ZERO);
            stats[j][1] = rate;
            j++;
        }

        return stats;
    }

    private static Object[][] getChartAvg(Map<Long, List<BigDecimal>> byTimeMap) {
        if (byTimeMap == null) return null;

        Object[][] stats = new Object[byTimeMap.size() + 1][2];
        stats[0][0] = "Time";
        stats[0][1] = "Avg";

        int j = 1;
        for (Map.Entry<Long, List<BigDecimal>> cycle : byTimeMap.entrySet()) {
            stats[j][0] = cycle.getKey();
            BigDecimal rate = cycle.getValue().stream()
                    .reduce(BigDecimal::add)
                    .orElse(BigDecimal.ZERO)
                    .divide(new BigDecimal(cycle.getValue().size()), 2, RoundingMode.HALF_EVEN);
            stats[j][1] = rate;
            j++;
        }

        return stats;
    }

    private Map<Long, List<BigDecimal>> createByTimeMap(JvmLogFile jvmLogFile) {
        List<GCLogCycleEntry> cycles = jvmLogFile.getGcLogFile().getCycleEntries();

        if (cycles.size() <= 1) {
            return null;
        }

        GCLogCycleEntry prev = null;
        GCLogCycleEntry current = null;
        int i = 0;

        Map<Long, List<BigDecimal>> byTimeMap = new LinkedHashMap<>();

        for (GCLogCycleEntry cycle : cycles) {
            prev = current;
            current = cycle;
            if (prev == null) {
                continue;
            }

            BigDecimal rate = new BigDecimal(current.getHeapBeforeGC() - prev.getHeapAfterGC()).divide(current.getTimeStamp().subtract(prev.getTimeStamp()), 2, RoundingMode.HALF_EVEN);

            long prevCycleMinute = prev.getTimeStamp().divide(minuteCount, 2, RoundingMode.HALF_EVEN).longValue();
            long currentCycleMinute = current.getTimeStamp().divide(minuteCount, 2, RoundingMode.HALF_EVEN).longValue();
            for (long j = prevCycleMinute; j <= currentCycleMinute; j++) {
                byTimeMap.computeIfAbsent(j, minute -> new ArrayList<>()).add(rate);
            }
        }
        return byTimeMap;
    }
}
