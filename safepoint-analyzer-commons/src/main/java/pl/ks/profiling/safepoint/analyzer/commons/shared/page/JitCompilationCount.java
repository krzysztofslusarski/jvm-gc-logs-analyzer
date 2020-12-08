package pl.ks.profiling.safepoint.analyzer.commons.shared.page;

import java.text.DecimalFormat;
import java.util.List;
import pl.ks.profiling.gui.commons.Chart;
import pl.ks.profiling.gui.commons.Page;
import pl.ks.profiling.safepoint.analyzer.commons.shared.pareser.jit.CompilationStatus;
import pl.ks.profiling.safepoint.analyzer.commons.shared.pareser.safepoint.SafepointLogFile;

public class JitCompilationCount implements PageCreator {
    @Override
    public Page create(SafepointLogFile safepointLogFile, DecimalFormat decimalFormat) {
        return Page.builder()
                .menuName("JIT compilation count")
                .fullName("JIT compilation count")
                .info("Following chart current compilation count.")
                .icon(Page.Icon.CHART)
                .pageContents(
                        List.of(
                                Chart.builder()
                                        .chartType(Chart.ChartType.LINE)
                                        .title("Current count")
                                        .data(getCurrentCountChart(safepointLogFile.getJitLogFile().getCompilationStatuses()))
                                        .build()
                        )
                )
                .build();
    }

    private static Object[][] getCurrentCountChart(List<CompilationStatus> compilationStatuses) {
        Object[][] stats = new Object[compilationStatuses.size() + 1][2];
        stats[0][0] = "Time";
        stats[0][1] = "Count";
        int i = 1;
        for (CompilationStatus status : compilationStatuses) {
            stats[i][0] = status.getTimeStamp();
            stats[i][1] = status.getCurrentCount();
            i++;
        }
        return stats;
    }

}
