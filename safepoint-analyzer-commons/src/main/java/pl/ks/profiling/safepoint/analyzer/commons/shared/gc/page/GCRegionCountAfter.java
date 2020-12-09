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

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import pl.ks.profiling.gui.commons.Chart;
import pl.ks.profiling.gui.commons.Page;
import pl.ks.profiling.safepoint.analyzer.commons.shared.JvmLogFile;
import pl.ks.profiling.safepoint.analyzer.commons.shared.PageCreator;
import pl.ks.profiling.safepoint.analyzer.commons.shared.gc.parser.GCLogCycleEntry;

public class GCRegionCountAfter implements PageCreator {
    @Override
    public Page create(JvmLogFile jvmLogFile, DecimalFormat decimalFormat) {
        if (jvmLogFile.getGcLogFile().getStats().getGcRegions().isEmpty()) {
            return null;
        }
        return Page.builder()
                .menuName("GC region stats - after GC")
                .fullName("Garbage Collector region stats - after GC")
                .info("Page presents charts with count of G1 regions after Garbage Collection. Charts are generated for every Garbage Collector phase.")
                .icon(Page.Icon.CHART)
                .pageContents(
                        jvmLogFile.getGcLogFile().getStats().getGcAggregatedPhases().stream()
                                .map(phase -> Chart.builder()
                                        .chartType(Chart.ChartType.LINE)
                                        .title(phase)
                                        .data(getChart(phase, jvmLogFile))
                                        .build())
                                .filter(chart -> chart.getData() != null)
                                .collect(Collectors.toList())
                )
                .build();
    }

    private static Object[][] getChart(String aggregatedPhase, JvmLogFile jvmLogFile) {
        List<GCLogCycleEntry> cycles = jvmLogFile.getGcLogFile().getCycleEntries().stream()
                .filter(gcCycleInfo -> aggregatedPhase.equals(gcCycleInfo.getAggregatedPhase()))
                .collect(Collectors.toList());
        Set<String> regions = cycles.stream()
                .flatMap(gcCycleInfo -> gcCycleInfo.getRegionsAfterGC().keySet().stream())
                .collect(Collectors.toSet());
        if (regions.size() == 0) {
            return null;
        }
        List<String> regionsSorted = new ArrayList<>(regions);
        regionsSorted.sort(String::compareTo);
        Object[][] stats = new Object[cycles.size() + 1][regionsSorted.size() + 1];
        stats[0][0] = "GC sequence";
        int i = 1;
        for (String region : regionsSorted) {
            stats[0][i] = region;
            i++;
        }

        int j = 1;
        for (GCLogCycleEntry cycle : cycles) {
            stats[j][0] = cycle.getTimeStamp();
            i = 1;
            for (String region : regionsSorted) {
                stats[j][i] = cycle.getRegionsAfterGC().get(region);
                i++;
            }
            j++;
        }

        return stats;
    }
}
