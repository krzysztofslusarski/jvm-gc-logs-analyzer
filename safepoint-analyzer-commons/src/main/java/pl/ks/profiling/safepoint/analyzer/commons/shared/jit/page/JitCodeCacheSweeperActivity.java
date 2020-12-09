package pl.ks.profiling.safepoint.analyzer.commons.shared.jit.page;

import java.text.DecimalFormat;
import java.util.List;
import pl.ks.profiling.gui.commons.Chart;
import pl.ks.profiling.gui.commons.Page;
import pl.ks.profiling.safepoint.analyzer.commons.shared.JvmLogFile;
import pl.ks.profiling.safepoint.analyzer.commons.shared.PageCreator;
import pl.ks.profiling.safepoint.analyzer.commons.shared.jit.parser.CodeCacheSweeperActivity;

public class JitCodeCacheSweeperActivity implements PageCreator {
    @Override
    public Page create(JvmLogFile jvmLogFile, DecimalFormat decimalFormat) {
        return Page.builder()
                .menuName("CodeCache sweeper activity")
                .fullName("CodeCache sweeper activity")
                .info("Following charts shows CodeCache sweeper activity")
                .icon(Page.Icon.CHART)
                .pageContents(
                        List.of(
                                Chart.builder()
                                        .chartType(Chart.ChartType.LINE)
                                        .title("Current count")
                                        .data(getCurrentCountChart(jvmLogFile.getJitLogFile().getCodeCacheSweeperActivities()))
                                        .build()
                        )
                )
                .build();
    }

    private static Object[][] getCurrentCountChart(List<CodeCacheSweeperActivity> codeCacheSweeperActivities) {
        Object[][] stats = new Object[codeCacheSweeperActivities.size() + 1][2];
        stats[0][0] = "Time";
        stats[0][1] = "Count";
        int i = 1;
        for (CodeCacheSweeperActivity activity : codeCacheSweeperActivities) {
            stats[i][0] = activity.getTimeStamp();
            stats[i][1] = i;
            i++;
        }
        return stats;
    }

}
