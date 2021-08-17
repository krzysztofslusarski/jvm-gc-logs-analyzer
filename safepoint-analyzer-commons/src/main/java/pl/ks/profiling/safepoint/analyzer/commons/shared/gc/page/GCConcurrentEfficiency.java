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

public class GCConcurrentEfficiency implements PageCreator {
    @Override
    public Page create(JvmLogFile jvmLogFile, DecimalFormat decimalFormat) {
        if (jvmLogFile.getGcLogFile().getConcurrentCycleEntries().isEmpty()) {
            return null;
        }
        return Page.builder()
                .menuName("Concurrent phase efficiency")
                .fullName("Concurrent phase efficiency")
                .icon(Page.Icon.CHART)
                .pageContents(List.of(
                        Chart.builder()
                                .chartType(Chart.ChartType.POINTS)
                                .title("Mixed collections count after concurrent mark")
                                .info("Chart presents number of mixed collections after ending the concurrent phase")
                                .yAxisLabel("Mixed collections")
                                .data(getMixedCollectionCountChart(jvmLogFile))
                                .build(),
                        Chart.builder()
                                .chartType(Chart.ChartType.POINTS)
                                .title("Space reclaimed by remark phase")
                                .info("Chart presents space reclaimed by Remark phase")
                                .yAxisLabel("Reclaimed space")
                                .data(getRemarkReclaimedChart(jvmLogFile))
                                .build(),
                        Chart.builder()
                                .chartType(Chart.ChartType.LINE)
                                .title("Wasted concurrent cycles")
                                .info("Count of wasted concurrent cycles")
                                .data(getWastedCyclesChart(jvmLogFile))
                                .build()
                        )
                )
                .build();
    }

    private static Object[][] getWastedCyclesChart(JvmLogFile jvmLogFile) {
        List<GCLogConcurrentCycleEntry> cycles = jvmLogFile.getGcLogFile().getConcurrentCycleEntries();

        Object[][] stats = new Object[cycles.size() + 1][2];
        stats[0][0] = "GC sequence";
        stats[0][1] = "Wasted cycles";

        int j = 1;
        int count = 0;
        for (GCLogConcurrentCycleEntry cycle : cycles) {
            if (cycle.isWasted()) {
                count++;
            }
            stats[j][0] = cycle.getSequenceId();
            stats[j][1] = count;
            j++;
        }

        return stats;
    }

    private static Object[][] getRemarkReclaimedChart(JvmLogFile jvmLogFile) {
        List<GCLogConcurrentCycleEntry> cycles = jvmLogFile.getGcLogFile().getConcurrentCycleEntries();

        Object[][] stats = new Object[cycles.size() + 1][2];
        stats[0][0] = "GC sequence";
        stats[0][1] = "Remark reclaimed";

        int j = 1;
        for (GCLogConcurrentCycleEntry cycle : cycles) {
            stats[j][0] = cycle.getSequenceId();
            stats[j][1] = cycle.getRemarkReclaimed();
            j++;
        }

        return stats;
    }

    private static Object[][] getMixedCollectionCountChart(JvmLogFile jvmLogFile) {
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
