package pl.ks.profiling.safepoint.analyzer.commons.shared.page;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import pl.ks.profiling.gui.commons.Chart;
import pl.ks.profiling.gui.commons.Page;
import pl.ks.profiling.gui.commons.PageContent;
import pl.ks.profiling.safepoint.analyzer.commons.shared.pareser.gc.GcPhaseStats;
import pl.ks.profiling.safepoint.analyzer.commons.shared.pareser.gc.GcStats;
import pl.ks.profiling.safepoint.analyzer.commons.shared.pareser.safepoint.JvmLogFile;

public class GCPhaseTime implements PageCreator{
    @Override
    public Page create(JvmLogFile jvmLogFile, DecimalFormat decimalFormat) {
        List<PageContent> pageContents = new ArrayList<>();
        if (jvmLogFile.getGcStats().getGcAggregatedPhaseStats().size() < jvmLogFile.getGcStats().getGcPhaseStats().size()) {
            pageContents.add(Chart.builder()
                    .chartType(Chart.ChartType.PIE)
                    .title("Phase time (aggregated)")
                    .info("Phases with aggregation to major type of collection.")
                    .data(getGcAggregatedPhaseTimeChart(jvmLogFile.getGcStats()))
                    .build());
        }
        pageContents.add(Chart.builder()
                .chartType(Chart.ChartType.PIE)
                .title("Phase time")
                .info("Phases without aggregation.")
                .data(getGcPhaseTimeChart(jvmLogFile.getGcStats()))
                .build());

        return Page.builder()
                .menuName("GC phase time")
                .fullName("Garbage collector phase time")
                .info("These charts presents total time in each Stop The World phases of Garbage Collector.")
                .icon(Page.Icon.CHART)
                .pageContents(pageContents)
                .build();
    }

    private static Object[][] getGcAggregatedPhaseTimeChart(GcStats gcStats) {
        List<GcPhaseStats> gcPhaseStats = gcStats.getGcAggregatedPhaseStats();
        Object[][] stats = new Object[gcPhaseStats.size() + 1][2];
        stats[0][0] = "Phase name";
        stats[0][1] = "Total time";
        int i = 1;
        for (GcPhaseStats stat : gcPhaseStats) {
            stats[i][0] = stat.getName();
            stats[i][1] = stat.getTime().getTotal();
            i++;
        }
        return stats;
    }

    private static Object[][] getGcPhaseTimeChart(GcStats gcStats) {
        List<GcPhaseStats> gcPhaseStats = gcStats.getGcPhaseStats();
        Object[][] stats = new Object[gcPhaseStats.size() + 1][2];
        stats[0][0] = "Phase name";
        stats[0][1] = "Total time";
        int i = 1;
        for (GcPhaseStats stat : gcPhaseStats) {
            stats[i][0] = stat.getName();
            stats[i][1] = stat.getTime().getTotal();
            i++;
        }
        return stats;
    }
}
