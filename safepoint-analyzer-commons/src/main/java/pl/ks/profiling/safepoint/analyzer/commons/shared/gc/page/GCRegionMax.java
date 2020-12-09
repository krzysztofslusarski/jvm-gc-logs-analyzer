package pl.ks.profiling.safepoint.analyzer.commons.shared.gc.page;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import pl.ks.profiling.gui.commons.Chart;
import pl.ks.profiling.gui.commons.Page;
import pl.ks.profiling.safepoint.analyzer.commons.shared.JvmLogFile;
import pl.ks.profiling.safepoint.analyzer.commons.shared.PageCreator;
import pl.ks.profiling.safepoint.analyzer.commons.shared.gc.parser.GCLogCycleEntry;

public class GCRegionMax implements PageCreator {
    @Override
    public Page create(JvmLogFile jvmLogFile, DecimalFormat decimalFormat) {
        if (jvmLogFile.getGcLogFile().getStats().getGcRegions().isEmpty()) {
            return null;
        }
        return Page.builder()
                .menuName("GC region stats - max no or regions")
                .fullName("Garbage Collector region stats - max number or regions")
                .info("Chart presents what G1 decided is current max of count of regions by type.")
                .icon(Page.Icon.CHART)
                .pageContents(List.of(
                        Chart.builder()
                                .title("Max number or regions")
                                .chartType(Chart.ChartType.POINTS)
                                .data(getChart(jvmLogFile))
                                .build())
                )
                .build();
    }

    private static Object[][] getChart(JvmLogFile jvmLogFile) {
        List<GCLogCycleEntry> cycles = jvmLogFile.getGcLogFile().getCycleEntries();
        Set<String> regions = cycles.stream()
                .flatMap(gcCycleInfo -> gcCycleInfo.getRegionsMax().keySet().stream())
                .collect(Collectors.toSet());
        if (regions.size() == 0) {
            return null;
        }
        List<String> regionsSorted = new ArrayList<>(regions);
        regionsSorted.sort(String::compareTo);
        Object[][] stats = new Object[cycles.size() + 1][regionsSorted.size() + 1];
        stats[0][0] = "GC sequence";
        int i = 1;
        for (String region : regionsSorted) {
            stats[0][i] = region;
            i++;
        }

        int j = 1;
        for (GCLogCycleEntry cycle : cycles) {
            stats[j][0] = cycle.getTimeStamp();
            i = 1;
            for (String region : regionsSorted) {
                Integer maxRegion = cycle.getRegionsMax().get(region);
                if (maxRegion == null && j > 1) {
                    maxRegion = (Integer) stats[j - 1][i];
                }
                stats[j][i] = maxRegion;
                i++;
            }
            j++;
        }

        return stats;
    }
}
