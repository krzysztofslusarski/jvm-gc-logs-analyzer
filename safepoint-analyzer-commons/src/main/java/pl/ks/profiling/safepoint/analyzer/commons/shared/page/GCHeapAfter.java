package pl.ks.profiling.safepoint.analyzer.commons.shared.page;

import java.text.DecimalFormat;
import java.util.List;
import java.util.stream.Collectors;
import pl.ks.profiling.gui.commons.Chart;
import pl.ks.profiling.gui.commons.Page;
import pl.ks.profiling.safepoint.analyzer.commons.shared.pareser.gc.GcCycleInfo;
import pl.ks.profiling.safepoint.analyzer.commons.shared.pareser.safepoint.JvmLogFile;

public class GCHeapAfter implements PageCreator {
    @Override
    public Page create(JvmLogFile jvmLogFile, DecimalFormat decimalFormat) {
        return Page.builder()
                .menuName("Heap after GC")
                .fullName("Heap after GC")
                .info("These charts presents heap size after Garbage Collection. There are displayed only young, mixed and full collections. " +
                        "There are not displayed any piggybacked and concurrent collections.")
                .icon(Page.Icon.CHART)
                .pageContents(List.of(
                        Chart.builder()
                                .chartType(Chart.ChartType.LINE)
                                .title("Heap after GC")
                                .data(getHeapAfterGCSizeChart(jvmLogFile))
                                .build()
                ))
                .build();
    }

    private static Object[][] getHeapAfterGCSizeChart(JvmLogFile jvmLogFile) {
        List<GcCycleInfo> cyclesToShow = jvmLogFile.getGcLogFile().getGcCycleInfos()
                .stream()
                .filter(GcCycleInfo::isGenuineCollection)
                .collect(Collectors.toList());
        Object[][] stats = new Object[cyclesToShow.size() + 1][3];
        stats[0][0] = "Cycle";
        stats[0][1] = "Heap after GC";
        stats[0][2] = "Heap size";
        int i = 1;

        for (GcCycleInfo gcCycleInfo : cyclesToShow) {
            stats[i][0] = gcCycleInfo.getTimeStamp();
            stats[i][1] = gcCycleInfo.getHeapAfterGC();
            stats[i][2] = gcCycleInfo.getHeapSize();
            i++;
        }

        return stats;
    }
}
