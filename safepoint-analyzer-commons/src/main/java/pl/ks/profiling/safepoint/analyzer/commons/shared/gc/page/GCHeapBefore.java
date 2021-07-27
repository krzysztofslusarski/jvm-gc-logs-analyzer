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
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import pl.ks.profiling.gui.commons.Chart;
import pl.ks.profiling.gui.commons.Page;
import pl.ks.profiling.gui.commons.PageContent;
import pl.ks.profiling.safepoint.analyzer.commons.shared.PageCreator;
import pl.ks.profiling.safepoint.analyzer.commons.shared.PageUtils;
import pl.ks.profiling.safepoint.analyzer.commons.shared.gc.parser.GCLogCycleEntry;
import pl.ks.profiling.safepoint.analyzer.commons.shared.report.JvmLogFile;

public class GCHeapBefore implements PageCreator {
    @Override
    public Page create(JvmLogFile jvmLogFile, DecimalFormat decimalFormat) {
        boolean hasNotGenuine = jvmLogFile.getGcLogFile().getCycleEntries()
                .stream()
                .anyMatch(gcLogCycleEntry -> !gcLogCycleEntry.isGenuineCollection());
        List<PageContent> charts = new ArrayList<>(3);
        if (hasNotGenuine) {
            charts.add(
                    Chart.builder()
                            .chartType(Chart.ChartType.POINTS_OR_LINE)
                            .seriesTypes(new Chart.SeriesType[] {Chart.SeriesType.POINTS, Chart.SeriesType.LINE})
                            .title("Heap before GC - genuine collections")
                            .info("Collections that are not \"genuine\": remark, cleanup, all piggybacks, initiated by too many humongous objects")
                            .xAxisLabel("Seconds since application start")
                            .yAxisLabel("MB")
                            .forceZeroMinValue(true)
                            .xAxisColumnIndex(chartColumns.indexOf(TIMESTAMP_COLUMN))
                            .data(getHeapBeforeGCSizeChart(jvmLogFile))
                            .build()
            );
        }

        charts.add(
                Chart.builder()
                        .chartType(Chart.ChartType.POINTS_OR_LINE)
                        .seriesTypes(new Chart.SeriesType[]{Chart.SeriesType.POINTS, Chart.SeriesType.LINE})
                        .title("Heap before GC - all collections")
                        .xAxisLabel("Seconds since application start")
                        .yAxisLabel("MB")
                        .forceZeroMinValue(true)
                        .xAxisColumnIndex(chartColumns.indexOf(TIMESTAMP_COLUMN))
                        .data(getHeapBeforeGCAllCollectionsSizeChart(jvmLogFile))
                        .build()
        );

        return Page.builder()
                .menuName("Heap before GC")
                .fullName("Heap before GC")
                .info("These charts presents heap size before Garbage Collection. There are displayed only young, mixed and full collections. " +
                        "There are not displayed any piggybacked and concurrent collections.")
                .icon(Page.Icon.CHART)
                .pageContents(charts)
                .build();
    }

    private static final String TIMESTAMP_COLUMN = "Timestamp";

    private static final List<String> chartColumns = List.of(
            TIMESTAMP_COLUMN,
            "Heap before GC",
            "Heap size");

    private static final List<Function<GCLogCycleEntry, Object>> chartExtractors = List.of(
            GCLogCycleEntry::getTimeStamp,
            GCLogCycleEntry::getHeapBeforeGCMb,
            GCLogCycleEntry::getHeapSizeMb);

    private static Object[][] getHeapBeforeGCSizeChart(JvmLogFile jvmLogFile) {
        List<GCLogCycleEntry> cyclesToShow = jvmLogFile.getGcLogFile().getCycleEntries()
                .stream()
                .filter(GCLogCycleEntry::isGenuineCollection)
                .collect(Collectors.toList());

        return PageUtils.toMatrix(cyclesToShow, chartColumns, chartExtractors);
    }

    private static Object[][] getHeapBeforeGCAllCollectionsSizeChart(JvmLogFile jvmLogFile) {
        List<GCLogCycleEntry> cyclesToShow = jvmLogFile.getGcLogFile().getCycleEntries();
        return PageUtils.toMatrix(cyclesToShow, chartColumns, chartExtractors);
    }
}
