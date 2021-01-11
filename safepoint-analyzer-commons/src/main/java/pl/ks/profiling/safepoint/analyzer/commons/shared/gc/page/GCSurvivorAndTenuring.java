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

import java.text.DecimalFormat;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import pl.ks.profiling.gui.commons.Chart;
import pl.ks.profiling.gui.commons.Page;
import pl.ks.profiling.safepoint.analyzer.commons.shared.report.JvmLogFile;
import pl.ks.profiling.safepoint.analyzer.commons.shared.PageCreator;
import pl.ks.profiling.safepoint.analyzer.commons.shared.PageUtils;
import pl.ks.profiling.safepoint.analyzer.commons.shared.gc.parser.GCLogCycleEntry;

public class GCSurvivorAndTenuring implements PageCreator {
    @Override
    public Page create(JvmLogFile jvmLogFile, DecimalFormat decimalFormat) {
        boolean dataPresent = jvmLogFile.getGcLogFile().getCycleEntries()
                .stream()
                .anyMatch(gcLogCycleEntry -> gcLogCycleEntry.getNewTenuringThreshold() > 0 || gcLogCycleEntry.getDesiredSurvivorSize() > 0);
        if (!dataPresent) {
            return null;
        }

        return Page.builder()
                .menuName("Tenuring threshold/survivor size")
                .fullName("Tenuring threshold/survivor size")
                .info("These charts presents desired survivor size and tenuring threshold calculated by GC")
                .icon(Page.Icon.CHART)
                .pageContents(List.of(
                        Chart.builder()
                                .chartType(Chart.ChartType.POINTS)
                                .title("Tenuring threshold")
                                .xAxisLabel("Seconds since application start")
                                .yAxisLabel("Tenuring threshold")
                                .data(getTenuringThreshold(jvmLogFile))
                                .build(),
                        Chart.builder()
                                .chartType(Chart.ChartType.POINTS)
                                .title("Desired survivor size")
                                .xAxisLabel("Seconds since application start")
                                .yAxisLabel("Desired survivor size")
                                .data(getDesiredSurvivorSize(jvmLogFile))
                                .build()
                ))
                .build();
    }

    private static final List<String> desiredSurvivorSizeChartColumns = List.of(
            "Cycle",
            "Size");

    private static final List<Function<GCLogCycleEntry, Object>> desiredSurvivorSizeChartExtractors = List.of(
            GCLogCycleEntry::getTimeStamp,
            GCLogCycleEntry::getDesiredSurvivorSize);

    private static Object[][] getDesiredSurvivorSize(JvmLogFile jvmLogFile) {
        List<GCLogCycleEntry> cyclesToShow = jvmLogFile.getGcLogFile().getCycleEntries()
                .stream()
                .filter(gcLogCycleEntry -> gcLogCycleEntry.getDesiredSurvivorSize() > 0)
                .collect(Collectors.toList());
        return PageUtils.toMatrix(cyclesToShow, desiredSurvivorSizeChartColumns, desiredSurvivorSizeChartExtractors);
    }

    private static final List<String> tenuringThresholdChartColumns = List.of(
            "Cycle",
            "Calculated size");

    private static final List<Function<GCLogCycleEntry, Object>> tenuringThresholdChartExtractors = List.of(
            GCLogCycleEntry::getTimeStamp,
            GCLogCycleEntry::getNewTenuringThreshold);

    private static Object[][] getTenuringThreshold(JvmLogFile jvmLogFile) {
        List<GCLogCycleEntry> cyclesToShow = jvmLogFile.getGcLogFile().getCycleEntries()
                .stream()
                .filter(gcLogCycleEntry -> gcLogCycleEntry.getNewTenuringThreshold() > 0)
                .collect(Collectors.toList());
        return PageUtils.toMatrix(cyclesToShow, tenuringThresholdChartColumns, tenuringThresholdChartExtractors);
    }
}
