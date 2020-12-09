/*
 * Copyright 2020 Krzysztof Slusarski
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package pl.ks.profiling.safepoint.analyzer.commons.shared.gc.page;

import java.text.DecimalFormat;
import java.util.List;
import java.util.stream.Collectors;
import pl.ks.profiling.gui.commons.Chart;
import pl.ks.profiling.gui.commons.Page;
import pl.ks.profiling.safepoint.analyzer.commons.shared.JvmLogFile;
import pl.ks.profiling.safepoint.analyzer.commons.shared.PageCreator;
import pl.ks.profiling.safepoint.analyzer.commons.shared.gc.parser.GCLogCycleEntry;

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
        List<GCLogCycleEntry> cyclesToShow = jvmLogFile.getGcLogFile().getCycleEntries()
                .stream()
                .filter(GCLogCycleEntry::isGenuineCollection)
                .collect(Collectors.toList());
        Object[][] stats = new Object[cyclesToShow.size() + 1][4];
        stats[0][0] = "Cycle";
        stats[0][1] = "Heap before GC";
        stats[0][2] = "Heap after GC";
        stats[0][3] = "Heap size";
        int i = 1;

        for (GCLogCycleEntry gcCycleInfo : cyclesToShow) {
            stats[i][0] = gcCycleInfo.getTimeStamp();
            stats[i][1] = gcCycleInfo.getHeapBeforeGC();
            stats[i][2] = gcCycleInfo.getHeapAfterGC();
            stats[i][3] = gcCycleInfo.getHeapSize();
            i++;
        }

        return stats;
    }

    private static Object[][] getReclaimedSizeChart(JvmLogFile jvmLogFile) {
        List<GCLogCycleEntry> cyclesToShow = jvmLogFile.getGcLogFile().getCycleEntries()
                .stream()
                .filter(GCLogCycleEntry::isGenuineCollection)
                .collect(Collectors.toList());
        Object[][] stats = new Object[cyclesToShow.size() + 1][2];
        stats[0][0] = "Cycle";
        stats[0][1] = "Reclaimed space";
        int i = 1;

        for (GCLogCycleEntry gcCycleInfo : cyclesToShow) {
            stats[i][0] = gcCycleInfo.getTimeStamp();
            stats[i][1] = gcCycleInfo.getHeapBeforeGC() - gcCycleInfo.getHeapAfterGC();
            i++;
        }

        return stats;
    }

    private static Object[][] getHeapBeforeGCSizeChart(JvmLogFile jvmLogFile) {
        List<GCLogCycleEntry> cyclesToShow = jvmLogFile.getGcLogFile().getCycleEntries()
                .stream()
                .filter(GCLogCycleEntry::isGenuineCollection)
                .collect(Collectors.toList());
        Object[][] stats = new Object[cyclesToShow.size() + 1][3];
        stats[0][0] = "Cycle";
        stats[0][1] = "Heap before GC";
        stats[0][2] = "Heap size";
        int i = 1;

        for (GCLogCycleEntry gcCycleInfo : cyclesToShow) {
            stats[i][0] = gcCycleInfo.getTimeStamp();
            stats[i][1] = gcCycleInfo.getHeapBeforeGC();
            stats[i][2] = gcCycleInfo.getHeapSize();
            i++;
        }

        return stats;
    }

    private static Object[][] getHeapAfterGCSizeChart(JvmLogFile jvmLogFile) {
        List<GCLogCycleEntry> cyclesToShow = jvmLogFile.getGcLogFile().getCycleEntries()
                .stream()
                .filter(GCLogCycleEntry::isGenuineCollection)
                .collect(Collectors.toList());
        Object[][] stats = new Object[cyclesToShow.size() + 1][3];
        stats[0][0] = "Cycle";
        stats[0][1] = "Heap after GC";
        stats[0][2] = "Heap size";
        int i = 1;

        for (GCLogCycleEntry gcCycleInfo : cyclesToShow) {
            stats[i][0] = gcCycleInfo.getTimeStamp();
            stats[i][1] = gcCycleInfo.getHeapAfterGC();
            stats[i][2] = gcCycleInfo.getHeapSize();
            i++;
        }

        return stats;
    }
}
