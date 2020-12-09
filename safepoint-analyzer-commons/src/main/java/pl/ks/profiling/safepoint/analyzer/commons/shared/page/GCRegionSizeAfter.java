package pl.ks.profiling.safepoint.analyzer.commons.shared.page;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import pl.ks.profiling.gui.commons.Chart;
import pl.ks.profiling.gui.commons.Page;
import pl.ks.profiling.safepoint.analyzer.commons.shared.parser.JvmLogFile;
import pl.ks.profiling.safepoint.analyzer.commons.shared.parser.gc.GcCycleInfo;

public class GCRegionSizeAfter implements PageCreator {
    @Override
    public Page create(JvmLogFile jvmLogFile, DecimalFormat decimalFormat) {
        if (jvmLogFile.getGcStats().getGcRegions().isEmpty()) {
            return null;
        }

        if (jvmLogFile.getGcLogFile().getGcCycleInfos().stream()
                .allMatch(gcCycleInfo -> gcCycleInfo.getRegionsSizeAfterGC().isEmpty())) {
            return null;
        };

        return Page.builder()
                .menuName("GC region sizes - after GC")
                .fullName("Garbage Collector region sizes - after GC")
                .info("Page presents charts with sizes of G1 regions after Garbage Collection.")
                .icon(Page.Icon.CHART)
                .pageContents(List.of(
                        Chart.builder()
                                .chartType(Chart.ChartType.LINE)
                                .title("Used space (in KB)")
                                .data(getUsedChart(jvmLogFile))
                                .build(),
                        Chart.builder()
                                .chartType(Chart.ChartType.LINE)
                                .title("Wasted space (in KB)")
                                .data(getWastedChart(jvmLogFile))
                                .build()
                        )
                )
                .build();
    }

    private static Object[][] getUsedChart(JvmLogFile jvmLogFile) {
        List<GcCycleInfo> cycles = jvmLogFile.getGcLogFile().getGcCycleInfos().stream()
                .filter(gcCycleInfo -> !gcCycleInfo.getRegionsSizeAfterGC().isEmpty())
                .collect(Collectors.toList());
        Set<String> regions = cycles.stream()
                .flatMap(gcCycleInfo -> gcCycleInfo.getRegionsSizeAfterGC().keySet().stream())
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
        for (GcCycleInfo cycle : cycles) {
            stats[j][0] = cycle.getTimeStamp();
            i = 1;
            for (String region : regionsSorted) {
                stats[j][i] = cycle.getRegionsSizeAfterGC().get(region);
                i++;
            }
            j++;
        }

        return stats;
    }

    private static Object[][] getWastedChart(JvmLogFile jvmLogFile) {
        List<GcCycleInfo> cycles = jvmLogFile.getGcLogFile().getGcCycleInfos().stream()
                .filter(gcCycleInfo -> !gcCycleInfo.getRegionsWastedAfterGC().isEmpty())
                .collect(Collectors.toList());
        Set<String> regions = cycles.stream()
                .flatMap(gcCycleInfo -> gcCycleInfo.getRegionsWastedAfterGC().keySet().stream())
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
        for (GcCycleInfo cycle : cycles) {
            stats[j][0] = cycle.getTimeStamp();
            i = 1;
            for (String region : regionsSorted) {
                stats[j][i] = cycle.getRegionsWastedAfterGC().get(region);
                i++;
            }
            j++;
        }

        return stats;
    }
}
