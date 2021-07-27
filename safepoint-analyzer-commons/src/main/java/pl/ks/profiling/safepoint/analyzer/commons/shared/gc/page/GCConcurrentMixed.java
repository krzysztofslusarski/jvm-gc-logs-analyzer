/*
 * Copyright 2021 Krzysztof Slusarski
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
import pl.ks.profiling.gui.commons.Chart;
import pl.ks.profiling.gui.commons.Page;
import pl.ks.profiling.safepoint.analyzer.commons.shared.PageCreator;
import pl.ks.profiling.safepoint.analyzer.commons.shared.gc.parser.GCLogConcurrentCycleEntry;
import pl.ks.profiling.safepoint.analyzer.commons.shared.report.JvmLogFile;

public class GCConcurrentMixed implements PageCreator {
    @Override
    public Page create(JvmLogFile jvmLogFile, DecimalFormat decimalFormat) {
        if (jvmLogFile.getGcLogFile().getConcurrentCycleEntries().isEmpty()) {
            return null;
        }
        return Page.builder()
                .menuName("Mixed after the concurrent phase")
                .fullName("Mixed after the concurrent phase")
                .info("Chart presents number of mixed collections after ending the concurrent phase")
                .icon(Page.Icon.CHART)
                .pageContents(List.of(
                        Chart.builder()
                                .chartType(Chart.ChartType.POINTS)
                                .yAxisLabel("Mixed collections")
                                .data(getChart(jvmLogFile))
                                .build())
                )
                .build();
    }

    private static Object[][] getChart(JvmLogFile jvmLogFile) {
        List<GCLogConcurrentCycleEntry> cycles = jvmLogFile.getGcLogFile().getConcurrentCycleEntries();

        Object[][] stats = new Object[cycles.size() + 1][2];
        stats[0][0] = "GC sequence";
        stats[0][1] = "Mixed collections";

        int j = 1;
        for (GCLogConcurrentCycleEntry cycle : cycles) {
            stats[j][0] = cycle.getSequenceId();
            stats[j][1] = cycle.getMixedCollectionsAfterConcurrent();
            j++;
        }

        return stats;
    }
}
