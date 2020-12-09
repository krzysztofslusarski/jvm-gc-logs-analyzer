package pl.ks.profiling.safepoint.analyzer.commons.shared.page;

import java.text.DecimalFormat;
import java.util.List;
import java.util.stream.Collectors;
import pl.ks.profiling.gui.commons.Chart;
import pl.ks.profiling.gui.commons.Page;
import pl.ks.profiling.safepoint.analyzer.commons.shared.pareser.gc.GcCycleInfo;
import pl.ks.profiling.safepoint.analyzer.commons.shared.pareser.safepoint.JvmLogFile;

public class GCHeapBeforeAfter implements PageCreator {
    @Override
    public Page create(JvmLogFile jvmLogFile, DecimalFormat decimalFormat) {
        return Page.builder()
                .menuName("Heap before/after GC")
                .fullName("Heap before/after GC")
                .info("These charts presents heap size before Garbage Collection, and after it. There are displayed only young, mixed and full collections. " +
                        "There are not displayed any piggybacked and concurrent collections.")
                .icon(Page.Icon.CHART)
                .pageContents(List.of(
                        Chart.builder()
                                .chartType(Chart.ChartType.POINTS_OR_LINE)
                                .seriesTypes(new Chart.SeriesType[]{Chart.SeriesType.POINTS, Chart.SeriesType.LINE, Chart.SeriesType.LINE})
                                .title("Heap before/after GC")
                                .data(getHeapSizeChart(jvmLogFile))
                                .build(),
                        Chart.builder()
                                .chartType(Chart.ChartType.POINTS)
                                .title("Reclaimed space")
                                .data(getReclaimedSizeChart(jvmLogFile))
                                .build()
                ))
                .build();
    }

    private static Object[][] getHeapSizeChart(JvmLogFile jvmLogFile) {
        List<GcCycleInfo> cyclesToShow = jvmLogFile.getGcLogFile().getGcCycleInfos()
                .stream()
                .filter(GcCycleInfo::isGenuineCollection)
                .collect(Collectors.toList());
        Object[][] stats = new Object[cyclesToShow.size() + 1][4];
        stats[0][0] = "Cycle";
        stats[0][1] = "Heap before GC";
        stats[0][2] = "Heap after GC";
        stats[0][3] = "Heap size";
        int i = 1;

        for (GcCycleInfo gcCycleInfo : cyclesToShow) {
            stats[i][0] = gcCycleInfo.getTimeStamp();
            stats[i][1] = gcCycleInfo.getHeapBeforeGC();
            stats[i][2] = gcCycleInfo.getHeapAfterGC();
            stats[i][3] = gcCycleInfo.getHeapSize();
            i++;
        }

        return stats;
    }

    private static Object[][] getReclaimedSizeChart(JvmLogFile jvmLogFile) {
        List<GcCycleInfo> cyclesToShow = jvmLogFile.getGcLogFile().getGcCycleInfos()
                .stream()
                .filter(GcCycleInfo::isGenuineCollection)
                .collect(Collectors.toList());
        Object[][] stats = new Object[cyclesToShow.size() + 1][2];
        stats[0][0] = "Cycle";
        stats[0][1] = "Reclaimed space";
        int i = 1;

        for (GcCycleInfo gcCycleInfo : cyclesToShow) {
            stats[i][0] = gcCycleInfo.getTimeStamp();
            stats[i][1] = gcCycleInfo.getHeapBeforeGC() - gcCycleInfo.getHeapAfterGC();
            i++;
        }

        return stats;
    }

    private static Object[][] getHeapBeforeGCSizeChart(JvmLogFile jvmLogFile) {
        List<GcCycleInfo> cyclesToShow = jvmLogFile.getGcLogFile().getGcCycleInfos()
                .stream()
                .filter(GcCycleInfo::isGenuineCollection)
                .collect(Collectors.toList());
        Object[][] stats = new Object[cyclesToShow.size() + 1][3];
        stats[0][0] = "Cycle";
        stats[0][1] = "Heap before GC";
        stats[0][2] = "Heap size";
        int i = 1;

        for (GcCycleInfo gcCycleInfo : cyclesToShow) {
            stats[i][0] = gcCycleInfo.getTimeStamp();
            stats[i][1] = gcCycleInfo.getHeapBeforeGC();
            stats[i][2] = gcCycleInfo.getHeapSize();
            i++;
        }

        return stats;
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
