package pl.ks.profiling.safepoint.analyzer.commons.shared.page;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.util.List;
import java.util.Set;
import pl.ks.profiling.gui.commons.Chart;
import pl.ks.profiling.gui.commons.Page;
import pl.ks.profiling.safepoint.analyzer.commons.shared.pareser.safepoint.SafepointLogFile;
import pl.ks.profiling.safepoint.analyzer.commons.shared.pareser.safepoint.SafepointOperationStats;
import pl.ks.profiling.safepoint.analyzer.commons.shared.pareser.safepoint.SafepointOperationStatsByName;

public class SafepoinOperationTime implements PageCreator{
    @Override
    public Page create(SafepointLogFile safepointLogFile, DecimalFormat decimalFormat) {
        SafepointOperationStats soStats = safepointLogFile.getSafepointOperationStats();
        return Page.builder()
                .menuName("SO time with TTS")
                .fullName("Safepoint operation time + avg(TTS)")
                .icon(Page.Icon.CHART)
                .pageContents(List.of(
                        Chart.builder()
                                .title("Safepoint operation time + avg(TTS)")
                                .info("Total time that your JVM wasted on Stop The World phases splited by Safepoint operation that caused it.")
                                .chartType(Chart.ChartType.PIE)
                                .data(getChart(soStats))
                                .build()
                ))
                .build();
    }

    private static Object[][] getChart(SafepointOperationStats safepointOperationStats) {
        Set<SafepointOperationStatsByName> statsByName = safepointOperationStats.getStatsByNames();
        Object[][] stats = new Object[statsByName.size() + 1][2];
        stats[0][0] = "Operation name";
        stats[0][1] = "Time with TTS";
        int i = 1;
        for (SafepointOperationStatsByName statByName : statsByName) {
            stats[i][0] = statByName.getOperationName();
            stats[i][1] = statByName.getOperationTime().getTotal()
                    .add(safepointOperationStats.getTts().getAverage().multiply(new BigDecimal(statByName.getCount())));
            i++;
        }
        return stats;
    }
}
