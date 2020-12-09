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

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.util.List;
import pl.ks.profiling.gui.commons.Chart;
import pl.ks.profiling.gui.commons.Page;
import pl.ks.profiling.safepoint.analyzer.commons.shared.JvmLogFile;
import pl.ks.profiling.safepoint.analyzer.commons.shared.PageCreator;
import pl.ks.profiling.safepoint.analyzer.commons.shared.gc.parser.GCLogCycleEntry;

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
                                .data(getChart(jvmLogFile))
                                .build())
                )
                .build();
    }

    private static Object[][] getChart(JvmLogFile jvmLogFile) {
        List<GCLogCycleEntry> cycles = jvmLogFile.getGcLogFile().getCycleEntries();

        if (cycles.size() <= 1) {
            return null;
        }

        BigDecimal[] allocationRate = new BigDecimal[cycles.size() -1];
        GCLogCycleEntry prev = null;
        GCLogCycleEntry current = null;
        int i = 0;
        for (GCLogCycleEntry cycle : cycles) {
            prev = current;
            current = cycle;
            if (prev == null) {
                continue;
            }

            allocationRate[i++] = new BigDecimal(current.getHeapBeforeGC() - prev.getHeapAfterGC()).divide(current.getTimeStamp().subtract(prev.getTimeStamp()), 2, RoundingMode.HALF_EVEN);
        }

        Object[][] stats = new Object[cycles.size()][2];
        stats[0][0] = "GC sequence";
        stats[0][1] = "Allocation rate";

        int j = 1;
        for (GCLogCycleEntry cycle : cycles) {
            if (j - 1 >= allocationRate.length) {
                break;
            }
            stats[j][0] = cycle.getTimeStamp();
            stats[j][1] = allocationRate[j - 1];
            j++;
        }

        return stats;
    }
}
