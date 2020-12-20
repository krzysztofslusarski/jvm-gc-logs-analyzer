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

import pl.ks.profiling.gui.commons.Chart;
import pl.ks.profiling.gui.commons.Page;
import pl.ks.profiling.gui.commons.PageContent;
import pl.ks.profiling.safepoint.analyzer.commons.shared.JvmLogFile;
import pl.ks.profiling.safepoint.analyzer.commons.shared.PageCreator;
import pl.ks.profiling.safepoint.analyzer.commons.shared.PageUtils;
import pl.ks.profiling.safepoint.analyzer.commons.shared.gc.parser.GCPhaseStats;
import pl.ks.profiling.safepoint.analyzer.commons.shared.gc.parser.GCStats;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

public class GCPhaseTime implements PageCreator {
    @Override
    public Page create(JvmLogFile jvmLogFile, DecimalFormat decimalFormat) {
        List<PageContent> pageContents = new ArrayList<>();
        if (jvmLogFile.getGcLogFile().getStats().getGcAggregatedPhaseStats().size() < jvmLogFile.getGcLogFile().getStats().getGcPhaseStats().size()) {
            pageContents.add(Chart.builder()
                    .chartType(Chart.ChartType.PIE)
                    .title("Phase time (aggregated)")
                    .info("Phases with aggregation to major type of collection.")
                    .data(getGcAggregatedPhaseTimeChart(jvmLogFile.getGcLogFile().getStats()))
                    .build());
        }
        pageContents.add(Chart.builder()
                .chartType(Chart.ChartType.PIE)
                .title("Phase time")
                .info("Phases without aggregation.")
                .data(getGcPhaseTimeChart(jvmLogFile.getGcLogFile().getStats()))
                .build());

        return Page.builder()
                .menuName("GC phase time")
                .fullName("Garbage collector phase time")
                .info("These charts presents total time in each Stop-the-world phases of Garbage Collector.")
                .icon(Page.Icon.CHART)
                .pageContents(pageContents)
                .build();
    }

    private static final List<String> heapAfterGcSizeChartColumns = List.of(
            "Phase name",
            "Total time");

    private static final Function<GCPhaseStats, Object> totalTime = (GCPhaseStats stat) -> stat.getTime().getTotal();
    private static final List<Function<GCPhaseStats, Object>> heapAfterGcSizeChartExtractors = List.of(
            GCPhaseStats::getName,
            totalTime);

    private static Object[][] getGcAggregatedPhaseTimeChart(GCStats gcStats) {
        return PageUtils.toMatrix(gcStats.getGcAggregatedPhaseStats(), heapAfterGcSizeChartColumns, heapAfterGcSizeChartExtractors);
    }

    private static Object[][] getGcPhaseTimeChart(GCStats gcStats) {
        return PageUtils.toMatrix(gcStats.getGcPhaseStats(), heapAfterGcSizeChartColumns, heapAfterGcSizeChartExtractors);
    }
}
