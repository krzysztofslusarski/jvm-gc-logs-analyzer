package pl.ks.profiling.safepoint.analyzer.commons.shared.page;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import pl.ks.profiling.gui.commons.Chart;
import pl.ks.profiling.gui.commons.Page;
import pl.ks.profiling.gui.commons.PageContent;
import pl.ks.profiling.safepoint.analyzer.commons.shared.pareser.gc.GcCycleInfo;
import pl.ks.profiling.safepoint.analyzer.commons.shared.pareser.safepoint.JvmLogFile;

public class GCRegionCountBeforeAndAfter implements PageCreator{
    @Override
    public Page create(JvmLogFile jvmLogFile, DecimalFormat decimalFormat) {
        if (jvmLogFile.getGcStats().getGcRegions().isEmpty()) {
            return null;
        }
        List<PageContent> charts = new ArrayList<>();
        for (String region : jvmLogFile.getGcStats().getGcRegions()) {
            charts.addAll(jvmLogFile.getGcStats().getGcAggregatedPhases().stream()
                    .map(phase -> Chart.builder()
                            .chartType(Chart.ChartType.LINE)
                            .title(phase + " (" + region + ")")
                            .data(getChart(phase, region, jvmLogFile))
                            .build())
                    .filter(chart -> chart.getData() != null)
                    .collect(Collectors.toList()));
        }
        return Page.builder()
                .menuName("GC region stats - before/after GC")
                .fullName("Garbage Collector region - before/after GC")
                .info("Page presents charts with count of G1 regions before and after Garbage Collection. Charts are generated for every Garbage Collector phase and region type.")
                .icon(Page.Icon.CHART)
                .pageContents(charts)
                .build();
    }

    private static Object[][] getChart(String aggregatedPhase, String region, JvmLogFile jvmLogFile) {
        List<GcCycleInfo> cycles = jvmLogFile.getGcLogFile().getGcCycleInfos().stream()
                .filter(gcCycleInfo -> aggregatedPhase.equals(gcCycleInfo.getAggregatedPhase()))
                .collect(Collectors.toList());
        Set<String> regions = cycles.stream()
                .flatMap(gcCycleInfo -> gcCycleInfo.getRegionsBeforeGC().keySet().stream())
                .collect(Collectors.toSet());
        if (regions.size() == 0) {
            return null;
        }
        Object[][] stats = new Object[cycles.size() + 1][3];
        stats[0][0] = "GC sequence";
        stats[0][1] = "Before";
        stats[0][2] = "After";
        int i = 1;

        int j = 1;
        for (GcCycleInfo cycle : cycles) {
            stats[j][0] = cycle.getTimeStamp();
            stats[j][1] = cycle.getRegionsBeforeGC().get(region);
            stats[j][2] = cycle.getRegionsAfterGC().get(region);
            j++;
        }

        return stats;
    }
}
