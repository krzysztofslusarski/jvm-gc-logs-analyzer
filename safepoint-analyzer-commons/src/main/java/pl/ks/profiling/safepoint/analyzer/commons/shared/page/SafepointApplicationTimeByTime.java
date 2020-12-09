package pl.ks.profiling.safepoint.analyzer.commons.shared.page;

import java.text.DecimalFormat;
import java.util.List;
import pl.ks.profiling.gui.commons.Chart;
import pl.ks.profiling.gui.commons.Page;
import pl.ks.profiling.safepoint.analyzer.commons.shared.pareser.safepoint.JvmLogFile;
import pl.ks.profiling.safepoint.analyzer.commons.shared.pareser.safepoint.SafepointOperationStats;
import pl.ks.profiling.safepoint.analyzer.commons.shared.pareser.safepoint.TimesInTime;

public class SafepointApplicationTimeByTime implements PageCreator {
    @Override
    public Page create(JvmLogFile jvmLogFile, DecimalFormat decimalFormat) {
        SafepointOperationStats soStats = jvmLogFile.getSafepointLogFile().getSafepointOperationStats();
        return Page.builder()
                .menuName("Application time (in time)")
                .fullName("Application time (in time)")
                .info("Following charts shows time when your application was running on your JVM in different window time.")
                .icon(Page.Icon.CHART)
                .pageContents(
                        List.of(
                                Chart.builder()
                                        .chartType(Chart.ChartType.LINE)
                                        .title("2 second window")
                                        .data(getChart(soStats.getTimesInTimes2sec()))
                                        .build(),
                                Chart.builder()
                                        .chartType(Chart.ChartType.LINE)
                                        .title("5 second window")
                                        .data(getChart(soStats.getTimesInTimes5sec()))
                                        .build(),
                                Chart.builder()
                                        .chartType(Chart.ChartType.LINE)
                                        .title("15 second window")
                                        .data(getChart(soStats.getTimesInTimes15sec()))
                                        .build()
                        )
                )
                .build();
    }

    private static Object[][] getChart(List<TimesInTime> timesInTimes) {
        Object[][] stats = new Object[timesInTimes.size() + 1][2];
        stats[0][0] = "Phase start time";
        stats[0][1] = "Application time";
        int i = 1;
        for (TimesInTime timesInTime : timesInTimes) {
            stats[i][0] = timesInTime.getStartTime();
            stats[i][1] = timesInTime.getApplicationTime();
            i++;
        }
        return stats;
    }
}
