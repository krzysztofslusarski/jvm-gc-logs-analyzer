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
import org.apache.commons.collections4.CollectionUtils;
import pl.ks.profiling.gui.commons.Chart;
import pl.ks.profiling.gui.commons.Page;
import pl.ks.profiling.safepoint.analyzer.commons.shared.PageCreator;
import pl.ks.profiling.safepoint.analyzer.commons.shared.gc.parser.GCLogCycleEntry;
import pl.ks.profiling.safepoint.analyzer.commons.shared.report.JvmLogFile;

public class GCRegionCountAfter implements PageCreator {
    @Override
    public Page create(JvmLogFile jvmLogFile, DecimalFormat decimalFormat) {
        if (jvmLogFile.getGcLogFile().getStats().getGcRegions().isEmpty()) {
            return null;
        }

        Set<String> regions = jvmLogFile.getGcLogFile().getCycleEntries().stream()
                .flatMap(gcCycleInfo -> gcCycleInfo.getRegionsBeforeGC().keySet().stream())
                .collect(Collectors.toSet());
        if (CollectionUtils.isEmpty(regions)) {
            return null;
        }
        List<String> regionsSorted = new ArrayList<>(regions);
        regionsSorted.sort(String::compareTo);

        return Page.builder()
                .menuName("GC region stats - after GC")
                .fullName("Garbage Collector region stats - after GC")
                .info("Page presents charts with count of G1 regions after Garbage Collection.")
                .icon(Page.Icon.CHART)
                .pageContents(
                            regionsSorted.stream()
                                .map(regionName -> Chart.builder()
                                        .chartType(Chart.ChartType.POINTS)
                                        .title(regionName)
                                        .data(getChart(regionName, jvmLogFile))
                                        .xAxisLabel("Seconds since application start when collection happened")
                                        .yAxisLabel("Number of regions after collection")
                                        .build())
                                .filter(chart -> chart.getData() != null)
                                .collect(Collectors.toList())
                )
                .build();
    }

    private static Object[][] getChart(String regionName, JvmLogFile jvmLogFile) {
        List<GCLogCycleEntry> cycles = jvmLogFile.getGcLogFile().getCycleEntries();

        Object[][] stats = new Object[cycles.size() + 1][2];
        stats[0][0] = "GC sequence";
        stats[0][1] = regionName;

        int j = 1;
        for (GCLogCycleEntry cycle : cycles) {
            stats[j][0] = cycle.getTimeStamp();
            stats[j][1] = cycle.getRegionsAfterGC().get(regionName);
            j++;
        }

        return stats;
    }
}
