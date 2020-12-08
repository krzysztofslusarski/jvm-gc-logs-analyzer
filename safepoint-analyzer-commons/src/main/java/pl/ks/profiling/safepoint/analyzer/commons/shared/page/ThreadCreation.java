package pl.ks.profiling.safepoint.analyzer.commons.shared.page;

import java.text.DecimalFormat;
import java.util.List;
import pl.ks.profiling.gui.commons.Chart;
import pl.ks.profiling.gui.commons.Page;
import pl.ks.profiling.safepoint.analyzer.commons.shared.pareser.safepoint.SafepointLogFile;
import pl.ks.profiling.safepoint.analyzer.commons.shared.pareser.thread.ThreadsStatus;

public class ThreadCreation implements PageCreator {
    @Override
    public Page create(SafepointLogFile safepointLogFile, DecimalFormat decimalFormat) {
        return Page.builder()
                .menuName("Thread creation")
                .fullName("Thread creation")
                .info("Following charts shows when threads are created.")
                .icon(Page.Icon.CHART)
                .pageContents(
                        List.of(
                                Chart.builder()
                                        .chartType(Chart.ChartType.LINE)
                                        .title("Created")
                                        .data(getCreatedChart(safepointLogFile.getThreadLogFile().getThreadsStatuses()))
                                        .build()
                        )
                )
                .build();
    }

    private static Object[][] getCreatedChart(List<ThreadsStatus> threadsStatuses) {
        Object[][] stats = new Object[threadsStatuses.size() + 1][2];
        stats[0][0] = "Time";
        stats[0][1] = "Created";
        int i = 1;
        for (ThreadsStatus status : threadsStatuses) {
            stats[i][0] = status.getTimeStamp();
            stats[i][1] = status.getCreatedCount();
            i++;
        }
        return stats;
    }

}
