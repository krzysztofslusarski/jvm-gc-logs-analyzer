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
package pl.ks.profiling.safepoint.analyzer.commons.shared.classloader.page;

import pl.ks.profiling.gui.commons.Chart;
import pl.ks.profiling.gui.commons.Page;
import pl.ks.profiling.safepoint.analyzer.commons.shared.report.JvmLogFile;
import pl.ks.profiling.safepoint.analyzer.commons.shared.PageCreator;
import pl.ks.profiling.safepoint.analyzer.commons.shared.PageUtils;
import pl.ks.profiling.safepoint.analyzer.commons.shared.classloader.parser.ClassStatus;

import java.text.DecimalFormat;
import java.util.List;
import java.util.function.Function;

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
                                        .xAxisLabel("Seconds since application start")
                                        .yAxisLabel("Current number of loaded classes counted since beginning of log files")
                                        .data(getCurrentCountChart(jvmLogFile.getClassLoaderLogFile().getClassStatuses()))
                                        .build(),
                                Chart.builder()
                                        .chartType(Chart.ChartType.LINE)
                                        .title("Loaded")
                                        .xAxisLabel("Seconds since application start")
                                        .yAxisLabel("Total number of classes loaded since beginning of log files")
                                        .data(getCreatedChart(jvmLogFile.getClassLoaderLogFile().getClassStatuses()))
                                        .build()
                        )
                )
                .build();
    }

    private static final List<String> currentCountChartColumns = List.of(
            "Time",
            "Count");
    private static final List<Function<ClassStatus, Object>> currentCountChartExtractors = List.of(
            ClassStatus::getTimeStamp,
            ClassStatus::getCurrentCount);

    private static Object[][] getCurrentCountChart(List<ClassStatus> entries) {
        return PageUtils.toMatrix(entries, currentCountChartColumns, currentCountChartExtractors);
    }

    private static final List<String> createdChartColumns = List.of(
            "Time",
            "Used");
    private static final List<Function<ClassStatus, Object>> createdChartExtractors = List.of(
            ClassStatus::getTimeStamp,
            ClassStatus::getLoadedCount);

    private static Object[][] getCreatedChart(List<ClassStatus> entries) {
        return PageUtils.toMatrix(entries, createdChartColumns, createdChartExtractors);
    }
}
