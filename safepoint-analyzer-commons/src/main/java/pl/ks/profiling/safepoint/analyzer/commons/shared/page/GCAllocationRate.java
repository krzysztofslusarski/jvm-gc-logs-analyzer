package pl.ks.profiling.safepoint.analyzer.commons.shared.page;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.util.List;
import pl.ks.profiling.gui.commons.Chart;
import pl.ks.profiling.gui.commons.Page;
import pl.ks.profiling.safepoint.analyzer.commons.shared.parser.JvmLogFile;
import pl.ks.profiling.safepoint.analyzer.commons.shared.parser.gc.GcCycleInfo;

public class GCAllocationRate implements PageCreator {
    @Override
    public Page create(JvmLogFile jvmLogFile, DecimalFormat decimalFormat) {
        if (jvmLogFile.getGcStats().getGcRegions().isEmpty()) {
            return null;
        }
        return Page.builder()
                .menuName("Allocation rate")
                .fullName("Allocation rate")
                .info("Chart presents allocation rate in MB/s.")
                .icon(Page.Icon.CHART)
                .pageContents(List.of(
                        Chart.builder()
                                .chartType(Chart.ChartType.POINTS)
                                .data(getChart(jvmLogFile))
                                .build())
                )
                .build();
    }

    private static Object[][] getChart(JvmLogFile jvmLogFile) {
        List<GcCycleInfo> cycles = jvmLogFile.getGcLogFile().getGcCycleInfos();

        if (cycles.size() <= 1) {
            return null;
        }

        BigDecimal[] allocationRate = new BigDecimal[cycles.size() -1];
        GcCycleInfo prev = null;
        GcCycleInfo current = null;
        int i = 0;
        for (GcCycleInfo cycle : cycles) {
            prev = current;
            current = cycle;
            if (prev == null) {
                continue;
            }

            allocationRate[i++] = new BigDecimal(current.getHeapBeforeGC() - prev.getHeapAfterGC()).divide(current.getTimeStamp().subtract(prev.getTimeStamp()), 2, RoundingMode.HALF_EVEN);
        }

        Object[][] stats = new Object[cycles.size()][2];
        stats[0][0] = "GC sequence";
        stats[0][1] = "Allocation rate";

        int j = 1;
        for (GcCycleInfo cycle : cycles) {
            if (j - 1 >= allocationRate.length) {
                break;
            }
            stats[j][0] = cycle.getTimeStamp();
            stats[j][1] = allocationRate[j - 1];
            j++;
        }

        return stats;
    }
}
