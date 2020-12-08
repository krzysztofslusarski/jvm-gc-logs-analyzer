package pl.ks.profiling.safepoint.analyzer.commons.shared.page;

import java.text.DecimalFormat;
import java.util.List;
import pl.ks.profiling.gui.commons.Chart;
import pl.ks.profiling.gui.commons.Page;
import pl.ks.profiling.safepoint.analyzer.commons.shared.pareser.jit.CodeCacheSweeperActivity;
import pl.ks.profiling.safepoint.analyzer.commons.shared.pareser.safepoint.SafepointLogFile;

public class JitCodeCacheSweeperActivity implements PageCreator {
    @Override
    public Page create(SafepointLogFile safepointLogFile, DecimalFormat decimalFormat) {
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
                                        .data(getCurrentCountChart(safepointLogFile.getJitLogFile().getCodeCacheSweeperActivities()))
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
