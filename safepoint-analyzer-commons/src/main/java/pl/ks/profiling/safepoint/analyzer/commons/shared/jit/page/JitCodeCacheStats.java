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

import pl.ks.profiling.gui.commons.Chart;
import pl.ks.profiling.gui.commons.Page;
import pl.ks.profiling.safepoint.analyzer.commons.shared.JvmLogFile;
import pl.ks.profiling.safepoint.analyzer.commons.shared.PageCreator;
import pl.ks.profiling.safepoint.analyzer.commons.shared.PageUtils;
import pl.ks.profiling.safepoint.analyzer.commons.shared.jit.parser.CodeCacheStatus;

import java.text.DecimalFormat;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

public class JitCodeCacheStats implements PageCreator {
    @Override
    public Page create(JvmLogFile jvmLogFile, DecimalFormat decimalFormat) {
        return Page.builder()
                .menuName("CodeCache stats")
                .fullName("CodeCache stats")
                .info("Following charts shows CodeCache stats in all segments.")
                .icon(Page.Icon.CHART)
                .pageContents(
                        jvmLogFile.getJitLogFile().getCodeCacheStatuses().entrySet().stream()
                                .map(entry ->
                                        Chart.builder()
                                                .chartType(Chart.ChartType.LINE)
                                                .title(entry.getKey())
                                                .data(getCurrentCountChart(entry.getValue()))
                                                .build()
                                )
                                .collect(Collectors.toList())
                )
                .build();
    }

    private static final List<String> currentCountChartColumns = List.of(
            "Time",
            "Size",
            "Max used",
            "Used");
    private static final List<Function<CodeCacheStatus, Object>> currentCountChartExtractors = List.of(
            CodeCacheStatus::getTimeStamp,
            CodeCacheStatus::getSize,
            CodeCacheStatus::getMaxUsed,
            CodeCacheStatus::getUsed);

    private static Object[][] getCurrentCountChart(List<CodeCacheStatus> entries) {
        return PageUtils.toMatrix(entries, currentCountChartColumns, currentCountChartExtractors);
    }

}
