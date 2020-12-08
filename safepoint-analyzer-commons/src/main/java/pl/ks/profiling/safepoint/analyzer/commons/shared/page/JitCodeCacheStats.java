package pl.ks.profiling.safepoint.analyzer.commons.shared.page;

import java.text.DecimalFormat;
import java.util.List;
import java.util.stream.Collectors;
import pl.ks.profiling.gui.commons.Chart;
import pl.ks.profiling.gui.commons.Page;
import pl.ks.profiling.safepoint.analyzer.commons.shared.pareser.jit.CodeCacheStatus;
import pl.ks.profiling.safepoint.analyzer.commons.shared.pareser.safepoint.SafepointLogFile;

public class JitCodeCacheStats implements PageCreator {
    @Override
    public Page create(SafepointLogFile safepointLogFile, DecimalFormat decimalFormat) {
        return Page.builder()
                .menuName("CodeCache stats")
                .fullName("CodeCache stats")
                .info("Following charts shows CodeCache stats in all segments.")
                .icon(Page.Icon.CHART)
                .pageContents(
                        safepointLogFile.getJitLogFile().getCodeCacheStatuses().entrySet().stream()
                                .map(entry ->
                                        Chart.builder()
                                                .chartType(Chart.ChartType.LINE)
                                                .title(entry.getKey())
                                                .data(getCurrentCountChart(entry.getValue()))
                                                .build()
                                )
                                .collect(Collectors.toList())
                )
                .build();
    }

    private static Object[][] getCurrentCountChart(List<CodeCacheStatus> codeCacheStatuses) {
        Object[][] stats = new Object[codeCacheStatuses.size() + 1][4];
        stats[0][0] = "Time";
        stats[0][1] = "Size";
        stats[0][2] = "Max used";
        stats[0][3] = "Used";
        int i = 1;
        for (CodeCacheStatus status : codeCacheStatuses) {
            stats[i][0] = status.getTimeStamp();
            stats[i][1] = status.getSize();
            stats[i][2] = status.getMaxUsed();
            stats[i][3] = status.getUsed();
            i++;
        }
        return stats;
    }

}
