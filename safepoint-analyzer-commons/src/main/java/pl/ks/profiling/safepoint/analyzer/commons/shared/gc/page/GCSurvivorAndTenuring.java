package pl.ks.profiling.safepoint.analyzer.commons.shared.gc.page;

import java.text.DecimalFormat;
import java.util.List;
import java.util.stream.Collectors;
import pl.ks.profiling.gui.commons.Chart;
import pl.ks.profiling.gui.commons.Page;
import pl.ks.profiling.safepoint.analyzer.commons.shared.JvmLogFile;
import pl.ks.profiling.safepoint.analyzer.commons.shared.PageCreator;
import pl.ks.profiling.safepoint.analyzer.commons.shared.gc.parser.GCLogCycleEntry;

public class GCSurvivorAndTenuring implements PageCreator {
    @Override
    public Page create(JvmLogFile jvmLogFile, DecimalFormat decimalFormat) {
        return Page.builder()
                .menuName("Tenuring threshold/survivor size")
                .fullName("Tenuring threshold/survivor size")
                .info("These charts presents desired survivor size and tenuring threshold calculated by GC")
                .icon(Page.Icon.CHART)
                .pageContents(List.of(
                        Chart.builder()
                                .chartType(Chart.ChartType.POINTS)
                                .title("Tenuring threshold")
                                .data(getTenuringThreshold(jvmLogFile))
                                .build(),
                        Chart.builder()
                                .chartType(Chart.ChartType.POINTS)
                                .title("Desired survivor size")
                                .data(getDesiredSurvivorSize(jvmLogFile))
                                .build()
                ))
                .build();
    }


    private static Object[][] getDesiredSurvivorSize(JvmLogFile jvmLogFile) {
        List<GCLogCycleEntry> cyclesToShow = jvmLogFile.getGcLogFile().getCycleEntries()
                .stream()
                .filter(gcLogCycleEntry -> gcLogCycleEntry.getDesiredSurvivorSize() > 0)
                .collect(Collectors.toList());
        Object[][] stats = new Object[cyclesToShow.size() + 1][2];
        stats[0][0] = "Cycle";
        stats[0][1] = "Size";
        int i = 1;

        for (GCLogCycleEntry gcCycleInfo : cyclesToShow) {
            stats[i][0] = gcCycleInfo.getTimeStamp();
            stats[i][1] = gcCycleInfo.getDesiredSurvivorSize();
            i++;
        }

        return stats;
    }

    private static Object[][] getTenuringThreshold(JvmLogFile jvmLogFile) {
        List<GCLogCycleEntry> cyclesToShow = jvmLogFile.getGcLogFile().getCycleEntries()
                .stream()
                .filter(gcLogCycleEntry -> gcLogCycleEntry.getMaxAge() > 0)
                .collect(Collectors.toList());
        Object[][] stats = new Object[cyclesToShow.size() + 1][2];
        stats[0][0] = "Cycle";
        stats[0][1] = "Calculated size";
        int i = 1;

        for (GCLogCycleEntry gcCycleInfo : cyclesToShow) {
            stats[i][0] = gcCycleInfo.getTimeStamp();
            stats[i][1] = gcCycleInfo.getNewTenuringThreshold();
            i++;
        }

        return stats;
    }
}
