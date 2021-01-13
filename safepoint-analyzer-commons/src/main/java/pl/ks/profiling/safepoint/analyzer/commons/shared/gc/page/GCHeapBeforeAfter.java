/*
 * Copyright 2020 Krzysztof Slusarski, Artur Owczarek
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
import java.util.function.Function;
import java.util.stream.Collectors;
import pl.ks.profiling.gui.commons.Chart;
import pl.ks.profiling.gui.commons.Page;
import pl.ks.profiling.safepoint.analyzer.commons.shared.report.JvmLogFile;
import pl.ks.profiling.safepoint.analyzer.commons.shared.PageCreator;
import pl.ks.profiling.safepoint.analyzer.commons.shared.PageUtils;
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
                                .xAxisLabel("Seconds since application start")
                                .yAxisLabel("Reclaimed MB")
                                .data(getHeapSizeChart(jvmLogFile))
                                .build(),
                        Chart.builder()
                                .chartType(Chart.ChartType.POINTS)
                                .title("Reclaimed space")
                                .xAxisLabel("Seconds since application start")
                                .yAxisLabel("MB")
                                .data(getReclaimedSizeChart(jvmLogFile))
                                .build()
                ))
                .build();
    }

    private static final List<String> heapSizeChartColumns = List.of(
            "Cycle",
            "Heap before GC",
            "Heap after GC",
            "Heap size");

    private static final List<Function<GCLogCycleEntry, Object>> heapSizeChartExtractors = List.of(
            GCLogCycleEntry::getTimeStamp,
            GCLogCycleEntry::getHeapBeforeGCMb,
            GCLogCycleEntry::getHeapAfterGCMb,
            GCLogCycleEntry::getHeapSizeMb);

    private static Object[][] getHeapSizeChart(JvmLogFile jvmLogFile) {
        return PageUtils.toMatrix(getEntries(jvmLogFile), heapSizeChartColumns, heapSizeChartExtractors);
    }

    private static List<GCLogCycleEntry> getEntries(JvmLogFile jvmLogFile) {
        return jvmLogFile.getGcLogFile().getCycleEntries()
                .stream()
                .filter(GCLogCycleEntry::isGenuineCollection)
                .collect(Collectors.toList());
    }

    private static final List<String> reclaimedSizeChartColumns = List.of(
            "Cycle",
            "Reclaimed space");

    private static final Function<GCLogCycleEntry, Object> reclaimedSpace = (GCLogCycleEntry gcCycleInfo) -> gcCycleInfo.getHeapBeforeGCMb() - gcCycleInfo.getHeapAfterGCMb();

    private static final List<Function<GCLogCycleEntry, Object>> reclaimedSizeChartExtractors = List.of(
            GCLogCycleEntry::getTimeStamp,
            reclaimedSpace);

    private static Object[][] getReclaimedSizeChart(JvmLogFile jvmLogFile) {
        return PageUtils.toMatrix(getEntries(jvmLogFile), reclaimedSizeChartColumns, reclaimedSizeChartExtractors);
    }

    private static final List<String> heapBeforeGcSizeChartColumns = List.of(
            "Cycle",
            "Heap before GC",
            "Heap size");

    private static final List<Function<GCLogCycleEntry, Object>> heapBeforeGcSizeChartExtractors = List.of(
            GCLogCycleEntry::getTimeStamp,
            GCLogCycleEntry::getHeapBeforeGCMb,
            GCLogCycleEntry::getHeapSizeMb);

    // TODO not used
    private static Object[][] getHeapBeforeGCSizeChart(JvmLogFile jvmLogFile) {
        return PageUtils.toMatrix(getEntries(jvmLogFile), heapBeforeGcSizeChartColumns, heapBeforeGcSizeChartExtractors);
    }

    private static final List<String> heapAfterGcSizeChartColumns = List.of(
            "Cycle",
            "Heap after GC",
            "Heap size");

    private static final List<Function<GCLogCycleEntry, Object>> heapAfterGcSizeChartExtractors = List.of(
            GCLogCycleEntry::getTimeStamp,
            GCLogCycleEntry::getHeapAfterGCMb,
            GCLogCycleEntry::getHeapSizeMb);

    // TODO not used
    private static Object[][] getHeapAfterGCSizeChart(JvmLogFile jvmLogFile) {
        return PageUtils.toMatrix(getEntries(jvmLogFile), heapAfterGcSizeChartColumns, heapAfterGcSizeChartExtractors);
    }
}
