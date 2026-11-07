/*
 * Copyright 2020 Krzysztof Slusarski
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
import pl.ks.profiling.safepoint.analyzer.commons.shared.PageCreator;
import pl.ks.profiling.safepoint.analyzer.commons.shared.gc.parser.GCLogCycleEntry;
import pl.ks.profiling.safepoint.analyzer.commons.shared.report.JvmLogFile;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

public class GCAllocationRate implements PageCreator {
    @Override
    public Page create(JvmLogFile jvmLogFile, DecimalFormat decimalFormat) {
        if (jvmLogFile.getGcLogFile().getStats().getGcRegions().isEmpty()) {
            return null;
        }
        return Page.builder()
                .menuName("Allocation rate")
                .fullName("Allocation rate")
                .info("Chart presents allocation rate in MB/s.")
                .icon(Page.Icon.CHART)
                .pageContents(List.of(
                        Chart.builder()
                                .chartType(Chart.ChartType.POINTS)
                                .xAxisLabel("Seconds since application start")
                                .yAxisLabel("Mb/s")
                                .data(getChart(jvmLogFile))
                                .build())
                )
                .build();
    }

    private static Object[][] getChart(JvmLogFile jvmLogFile) {
        List<GCLogCycleEntry> cycles = jvmLogFile.getGcLogFile().getCycleEntriesWithHeapSizes();
        if (cycles.size() <= 1) {
            return null;
        }

        // The rate between two collections is shown at the time of the first one
        List<Object[]> rows = new ArrayList<>();
        GCLogCycleEntry prev = null;
        for (GCLogCycleEntry cycle : cycles) {
            if (prev != null) {
                BigDecimal rate = GCLogCycleEntry.allocationRate(prev, cycle);
                if (rate != null) {
                    rows.add(new Object[]{prev.getTimeStamp(), rate});
                }
            }
            prev = cycle;
        }

        Object[][] stats = new Object[rows.size() + 1][];
        stats[0] = new Object[]{"GC sequence", "Allocation rate"};
        for (int i = 0; i < rows.size(); i++) {
            stats[i + 1] = rows.get(i);
        }
        return stats;
    }
}
