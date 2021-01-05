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
package pl.ks.profiling.safepoint.analyzer.commons.shared.jit.page;

import java.text.DecimalFormat;
import java.util.List;
import java.util.function.Function;

import pl.ks.profiling.gui.commons.Chart;
import pl.ks.profiling.gui.commons.Page;
import pl.ks.profiling.safepoint.analyzer.commons.shared.JvmLogFile;
import pl.ks.profiling.safepoint.analyzer.commons.shared.PageCreator;
import pl.ks.profiling.safepoint.analyzer.commons.shared.PageUtils;
import pl.ks.profiling.safepoint.analyzer.commons.shared.jit.parser.CompilationStatus;

public class JitCompilationCount implements PageCreator {
    @Override
    public Page create(JvmLogFile jvmLogFile, DecimalFormat decimalFormat) {
        return Page.builder()
                .menuName("JIT compilation count")
                .fullName("JIT compilation count")
                .info("Following chart current compilation count.")
                .icon(Page.Icon.CHART)
                .pageContents(
                        List.of(
                                Chart.builder()
                                        .chartType(Chart.ChartType.LINE)
                                        .title("Current count")
                                        .xAxisLabel("Seconds since application start")
                                        .yAxisLabel("Number of compilations")
                                        .data(getCurrentCountChart(jvmLogFile.getJitLogFile().getCompilationStatuses()))
                                        .build()
                        )
                )
                .build();
    }

    private static final List<String> currentCountChartColumns = List.of(
            "Time",
            "Count");
    private static final List<Function<CompilationStatus, Object>> currentCountChartExtractors = List.of(
            CompilationStatus::getTimeStamp,
            CompilationStatus::getCurrentCount);

    private static Object[][] getCurrentCountChart(List<CompilationStatus> entries) {
        return PageUtils.toMatrix(entries, currentCountChartColumns, currentCountChartExtractors);
    }

}
