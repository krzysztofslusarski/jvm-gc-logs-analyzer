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
import pl.ks.profiling.safepoint.analyzer.commons.shared.jit.parser.CompilationStatus;

import java.text.DecimalFormat;
import java.util.List;
import java.util.function.Function;

public class JitTieredCompilationCount implements PageCreator {
    @Override
    public Page create(JvmLogFile jvmLogFile, DecimalFormat decimalFormat) {
        return Page.builder()
                .menuName("JIT tiered compilation count")
                .fullName("JIT tiered compilation count")
                .info("Following chart current compilation count in each tier.")
                .icon(Page.Icon.CHART)
                .pageContents(
                        List.of(
                                Chart.builder()
                                        .chartType(Chart.ChartType.LINE)
                                        .title("Current tier count")
                                        .data(getCurrentCountChart(jvmLogFile.getJitLogFile().getCompilationStatuses()))
                                        .build(),
                                Chart.builder()
                                        .chartType(Chart.ChartType.LINE)
                                        .title("Current tier 2 count")
                                        .data(getTierCountChart(jvmLogFile.getJitLogFile().getCompilationStatuses(), 2))
                                        .build(),
                                Chart.builder()
                                        .chartType(Chart.ChartType.LINE)
                                        .title("Current tier 4 count")
                                        .data(getTierCountChart(jvmLogFile.getJitLogFile().getCompilationStatuses(), 4))
                                        .build()
                        )
                )
                .build();
    }

    private static Object[][] getCurrentCountChart(List<CompilationStatus> compilationStatuses) {
        Object[][] stats = new Object[compilationStatuses.size() + 1][5];
        stats[0][0] = "Time";
        stats[0][1] = "Tier 1";
        stats[0][2] = "Tier 2";
        stats[0][3] = "Tier 3";
        stats[0][4] = "Tier 4";
        int i = 1;
        for (CompilationStatus status : compilationStatuses) {
            stats[i][0] = status.getTimeStamp();
            stats[i][1] = status.getTier1CurrentCount();
            stats[i][2] = status.getTier2CurrentCount();
            stats[i][3] = status.getTier3CurrentCount();
            stats[i][4] = status.getTier4CurrentCount();
            i++;
        }
        return stats;
    }

    private static Object[][] getTierCountChart(List<CompilationStatus> entries, int tier) {
        Function<CompilationStatus, Object> getTierCount = (CompilationStatus status) -> {
            switch (tier) {
                case 1:
                    return status.getTier1CurrentCount();
                case 2:
                    return status.getTier2CurrentCount();
                case 3:
                    return status.getTier3CurrentCount();
                case 4:
                    return status.getTier4CurrentCount();
                default:
                    throw new IllegalStateException("Unexpected tier " + tier);
            }
        };
        List<String> columns = List.of(
                "Time",
                "Tier " + tier);
        List<Function<CompilationStatus, Object>> extractors = List.of(
                CompilationStatus::getTimeStamp,
                getTierCount
        );
        return PageUtils.toMatrix(entries, columns, extractors);
    }

}
