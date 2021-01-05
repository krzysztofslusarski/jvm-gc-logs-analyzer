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
package pl.ks.profiling.safepoint.analyzer.commons.shared.tlab.page;

import java.text.DecimalFormat;
import java.util.List;
import java.util.function.Function;

import pl.ks.profiling.gui.commons.Chart;
import pl.ks.profiling.gui.commons.Page;
import pl.ks.profiling.safepoint.analyzer.commons.shared.JvmLogFile;
import pl.ks.profiling.safepoint.analyzer.commons.shared.PageCreator;
import pl.ks.profiling.safepoint.analyzer.commons.shared.PageUtils;
import pl.ks.profiling.safepoint.analyzer.commons.shared.tlab.parser.TlabSummaryInfo;

public class TlabSummary implements PageCreator {
    @Override
    public Page create(JvmLogFile jvmLogFile, DecimalFormat decimalFormat) {
        return Page.builder()
                .menuName("TLAB summary")
                .fullName("TLAB summary")
                .info("Following charts shows TLAB summery.")
                .icon(Page.Icon.CHART)
                .pageContents(
                        List.of(
                                Chart.builder()
                                        .chartType(Chart.ChartType.POINTS)
                                        .title("Slow allocation of objects")
                                        .xAxisLabel("Seconds since application start")
                                        .yAxisLabel("Number of new slow allocations")
                                        .data(getSlowAllocationChart(jvmLogFile.getTlabLogFile().getTlabSummaries()))
                                        .build(),
                                Chart.builder()
                                        .chartType(Chart.ChartType.POINTS)
                                        .title("TLAB refills")
                                        .xAxisLabel("Seconds since application start")
                                        .yAxisLabel("Number of TLAB refills")
                                        .data(getRefillsChart(jvmLogFile.getTlabLogFile().getTlabSummaries()))
                                        .build(),
                                Chart.builder()
                                        .chartType(Chart.ChartType.POINTS)
                                        .title("Waste percent")
                                        .xAxisLabel("Seconds since application start")
                                        .yAxisLabel("Level of waste in %")
                                        .data(getWasteChart(jvmLogFile.getTlabLogFile().getTlabSummaries()))
                                        .build(),
                                Chart.builder()
                                        .chartType(Chart.ChartType.POINTS)
                                        .title("Number of allocating threads")
                                        .xAxisLabel("Seconds since application start")
                                        .yAxisLabel("Number of threads")
                                        .data(getAllocatingThreadsChart(jvmLogFile.getTlabLogFile().getTlabSummaries()))
                                        .build()
                        )
                )
                .build();
    }

    private static final List<String> allocatingThreadsChartColumns = List.of(
            "Time",
            "Allocation threads");
    private static final List<Function<TlabSummaryInfo, Object>> allocatingThreadsChartExtractors = List.of(
            TlabSummaryInfo::getTimeStamp,
            TlabSummaryInfo::getThreadCount);

    private static Object[][] getAllocatingThreadsChart(List<TlabSummaryInfo> entries) {
        return PageUtils.toMatrix(entries, allocatingThreadsChartColumns, allocatingThreadsChartExtractors);
    }

    private static final List<String> slowAllocationChartColumns = List.of(
            "Time",
            "Slow allocations",
            "One thread max slow allocations");
    private static final List<Function<TlabSummaryInfo, Object>> slowAllocationChartExtractors = List.of(
            TlabSummaryInfo::getTimeStamp,
            TlabSummaryInfo::getSlowAllocs,
            TlabSummaryInfo::getMaxSlowAllocs);

    private static Object[][] getSlowAllocationChart(List<TlabSummaryInfo> entries) {
        return PageUtils.toMatrix(entries, slowAllocationChartColumns, slowAllocationChartExtractors);
    }

    private static final List<String> refillsChartColumns = List.of(
            "Time",
            "Refills",
            "One thread max refills");
    private static final List<Function<TlabSummaryInfo, Object>> refillsChartExtractors = List.of(
            TlabSummaryInfo::getTimeStamp,
            TlabSummaryInfo::getRefills,
            TlabSummaryInfo::getMaxRefills);

    private static Object[][] getRefillsChart(List<TlabSummaryInfo> entries) {
        return PageUtils.toMatrix(entries, refillsChartColumns, refillsChartExtractors);
    }

    private static final List<String> wasteChartColumns = List.of(
            "Time",
            "Waste %");
    private static final List<Function<TlabSummaryInfo, Object>> wasteChartExtractors = List.of(
            TlabSummaryInfo::getTimeStamp,
            TlabSummaryInfo::getWastePercent);

    private static Object[][] getWasteChart(List<TlabSummaryInfo> entries) {
        return PageUtils.toMatrix(entries, wasteChartColumns, wasteChartExtractors);
    }
}
