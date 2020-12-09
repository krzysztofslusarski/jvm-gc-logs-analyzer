package pl.ks.profiling.safepoint.analyzer.commons.shared.classloader.page;

import java.text.DecimalFormat;
import java.util.List;
import pl.ks.profiling.gui.commons.Chart;
import pl.ks.profiling.gui.commons.Page;
import pl.ks.profiling.safepoint.analyzer.commons.shared.JvmLogFile;
import pl.ks.profiling.safepoint.analyzer.commons.shared.PageCreator;
import pl.ks.profiling.safepoint.analyzer.commons.shared.classloader.parser.ClassStatus;

public class ClassCount implements PageCreator {
    @Override
    public Page create(JvmLogFile jvmLogFile, DecimalFormat decimalFormat) {
        return Page.builder()
                .menuName("Class count/loading")
                .fullName("Class count/loading")
                .info("Following chart current class count and class loading activity.")
                .icon(Page.Icon.CHART)
                .pageContents(
                        List.of(
                                Chart.builder()
                                        .chartType(Chart.ChartType.LINE)
                                        .title("Current count")
                                        .data(getCurrentCountChart(jvmLogFile.getClassLoaderLogFile().getClassStatuses()))
                                        .build(),
                                Chart.builder()
                                        .chartType(Chart.ChartType.LINE)
                                        .title("Loaded")
                                        .data(getCreatedChart(jvmLogFile.getClassLoaderLogFile().getClassStatuses()))
                                        .build()
                        )
                )
                .build();
    }

    private static Object[][] getCurrentCountChart(List<ClassStatus> classStatuses) {
        Object[][] stats = new Object[classStatuses.size() + 1][2];
        stats[0][0] = "Time";
        stats[0][1] = "Count";
        int i = 1;
        for (ClassStatus status : classStatuses) {
            stats[i][0] = status.getTimeStamp();
            stats[i][1] = status.getCurrentCount();
            i++;
        }
        return stats;
    }

    private static Object[][] getCreatedChart(List<ClassStatus> classStatuses) {
        Object[][] stats = new Object[classStatuses.size() + 1][2];
        stats[0][0] = "Time";
        stats[0][1] = "Loaded";
        int i = 1;
        for (ClassStatus status : classStatuses) {
            stats[i][0] = status.getTimeStamp();
            stats[i][1] = status.getLoadedCount();
            i++;
        }
        return stats;
    }
}
