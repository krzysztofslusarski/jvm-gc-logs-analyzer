package pl.ks.profiling.safepoint.analyzer.commons.shared.page;

import java.text.DecimalFormat;
import java.util.List;
import pl.ks.profiling.gui.commons.Chart;
import pl.ks.profiling.gui.commons.Page;
import pl.ks.profiling.safepoint.analyzer.commons.shared.pareser.safepoint.JvmLogFile;
import pl.ks.profiling.safepoint.analyzer.commons.shared.pareser.thread.ThreadsStatus;

public class ThreadCount implements PageCreator {
    @Override
    public Page create(JvmLogFile jvmLogFile, DecimalFormat decimalFormat) {
        return Page.builder()
                .menuName("Thread count")
                .fullName("Thread count")
                .info("Following chart current thread count.")
                .icon(Page.Icon.CHART)
                .pageContents(
                        List.of(
                                Chart.builder()
                                        .chartType(Chart.ChartType.POINTS)
                                        .title("Current count")
                                        .data(getCurrentCountChart(jvmLogFile.getThreadLogFile().getThreadsStatuses()))
                                        .build()
                        )
                )
                .build();
    }

    private static Object[][] getCurrentCountChart(List<ThreadsStatus> threadsStatuses) {
        Object[][] stats = new Object[threadsStatuses.size() + 1][2];
        stats[0][0] = "Time";
        stats[0][1] = "Count";
        int i = 1;
        for (ThreadsStatus status : threadsStatuses) {
            stats[i][0] = status.getTimeStamp();
            stats[i][1] = status.getCurrentCount();
            i++;
        }
        return stats;
    }

}
