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
package pl.ks.profiling.safepoint.analyzer.commons.shared.thread.page;

import pl.ks.profiling.gui.commons.Chart;
import pl.ks.profiling.gui.commons.Page;
import pl.ks.profiling.safepoint.analyzer.commons.shared.report.JvmLogFile;
import pl.ks.profiling.safepoint.analyzer.commons.shared.PageCreator;
import pl.ks.profiling.safepoint.analyzer.commons.shared.PageUtils;
import pl.ks.profiling.safepoint.analyzer.commons.shared.thread.parser.ThreadsStatus;

import java.text.DecimalFormat;
import java.util.List;
import java.util.function.Function;

public class ThreadCount implements PageCreator {
    @Override
    public Page create(JvmLogFile jvmLogFile, DecimalFormat decimalFormat) {
        return Page.builder()
                .menuName("Thread count/creation")
                .fullName("Thread count/creation")
                .info("Following chart current thread count and creation.")
                .icon(Page.Icon.CHART)
                .pageContents(
                        List.of(
                                Chart.builder()
                                        .chartType(Chart.ChartType.POINTS)
                                        .title("Current count")
                                        .xAxisLabel("Seconds since application start")
                                        .yAxisLabel("Number of threads")
                                        .data(getCurrentCountChart(jvmLogFile.getThreadLogFile().getThreadsStatuses()))
                                        .build(),
                                Chart.builder()
                                        .chartType(Chart.ChartType.LINE)
                                        .title("Created")
                                        .xAxisLabel("Seconds since application start")
                                        .yAxisLabel("Number of created threads")
                                        .data(getCreatedChart(jvmLogFile.getThreadLogFile().getThreadsStatuses()))
                                        .build()
                        )
                )
                .build();
    }

    private static final List<String> currentChartColumns = List.of(
            "Time",
            "Count");
    private static final List<Function<ThreadsStatus, Object>> currentChartExtractors = List.of(
            ThreadsStatus::getTimeStamp,
            ThreadsStatus::getCurrentCount);

    private static Object[][] getCurrentCountChart(List<ThreadsStatus> entries) {
        return PageUtils.toMatrix(entries, currentChartColumns, currentChartExtractors);
    }

    private static final List<String> createdChartColumns = List.of(
            "Time",
            "Created");
    private static final List<Function<ThreadsStatus, Object>> createdChartExtractors = List.of(
            ThreadsStatus::getTimeStamp,
            ThreadsStatus::getCreatedCount);

    private static Object[][] getCreatedChart(List<ThreadsStatus> entries) {
        return PageUtils.toMatrix(entries, createdChartColumns, createdChartExtractors);
    }
}
