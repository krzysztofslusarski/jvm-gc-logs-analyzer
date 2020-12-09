package pl.ks.profiling.safepoint.analyzer.commons.shared.page;

import java.text.DecimalFormat;
import java.util.List;
import java.util.Set;
import pl.ks.profiling.gui.commons.Chart;
import pl.ks.profiling.gui.commons.Page;
import pl.ks.profiling.safepoint.analyzer.commons.shared.parser.JvmLogFile;
import pl.ks.profiling.safepoint.analyzer.commons.shared.parser.safepoint.SafepointOperationStats;
import pl.ks.profiling.safepoint.analyzer.commons.shared.parser.safepoint.SafepointOperationStatsByName;

public class SafepoinOperationCount implements PageCreator{
    @Override
    public Page create(JvmLogFile jvmLogFile, DecimalFormat decimalFormat) {
        SafepointOperationStats soStats = jvmLogFile.getSafepointLogFile().getSafepointOperationStats();
        return Page.builder()
                .menuName("SO count")
                .fullName("Safepoint operation count")
                .icon(Page.Icon.CHART)
                .pageContents(List.of(
                        Chart.builder()
                                .title("Safepoint operation count")
                                .info("Count of Safepoint operation that caused the Stop The World phase.")
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
        stats[0][1] = "Count";
        int i = 1;
        for (SafepointOperationStatsByName statByName : statsByName) {
            stats[i][0] = statByName.getOperationName();
            stats[i][1] = statByName.getCount();
            i++;
        }
        return stats;
    }
}
