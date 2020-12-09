package pl.ks.profiling.safepoint.analyzer.commons.shared.safepoint.page;

import java.text.DecimalFormat;
import java.util.List;
import java.util.Set;
import pl.ks.profiling.gui.commons.Chart;
import pl.ks.profiling.gui.commons.Page;
import pl.ks.profiling.safepoint.analyzer.commons.shared.JvmLogFile;
import pl.ks.profiling.safepoint.analyzer.commons.shared.PageCreator;
import pl.ks.profiling.safepoint.analyzer.commons.shared.safepoint.parser.SafepointOperationStats;
import pl.ks.profiling.safepoint.analyzer.commons.shared.safepoint.parser.SafepointOperationStatsByName;

public class SafepointTotalTimeInPhases implements PageCreator {
    @Override
    public Page create(JvmLogFile jvmLogFile, DecimalFormat decimalFormat) {
        SafepointOperationStats soStats = jvmLogFile.getSafepointLogFile().getSafepointOperationStats();
        return Page.builder()
                .menuName("Total time in phases")
                .fullName("Total time in phases")
                .icon(Page.Icon.CHART)
                .pageContents(List.of(
                        Chart.builder()
                                .chartType(Chart.ChartType.PIE)
                                .title("Total time in phases")
                                .info("Application time - time that your application is really running, Safepoint operation time - this is the time of Stop The World phase, Time to safepoint time - wasted time between JVM ordered Stop The World phase and real start of following phase.")
                                .data(getChart(soStats))
                                .build()
                ))
                .build();
    }

    private static Object[][] getChart(SafepointOperationStats safepointOperationStats) {
        Set<SafepointOperationStatsByName> statsByName = safepointOperationStats.getStatsByNames();
        Object[][] stats = new Object[4][2];
        stats[0][0] = "Phase name";
        stats[0][1] = "Total time";
        stats[1][0] = "Application time";
        stats[2][0] = "Safepoint operation time";
        stats[3][0] = "Time to safepoint time";
        stats[1][1] = safepointOperationStats.getApplicationTime().getTotal();
        stats[2][1] = safepointOperationStats.getOperationTime().getTotal();
        stats[3][1] = safepointOperationStats.getTts().getTotal();
        return stats;
    }
}
