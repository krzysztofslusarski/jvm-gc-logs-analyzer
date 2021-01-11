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
package pl.ks.profiling.safepoint.analyzer.commons.shared.stringdedup.page;

import pl.ks.profiling.gui.commons.Chart;
import pl.ks.profiling.gui.commons.Page;
import pl.ks.profiling.safepoint.analyzer.commons.shared.report.JvmLogFile;
import pl.ks.profiling.safepoint.analyzer.commons.shared.PageCreator;
import pl.ks.profiling.safepoint.analyzer.commons.shared.PageUtils;
import pl.ks.profiling.safepoint.analyzer.commons.shared.stringdedup.parser.StringDedupLogEntry;

import java.text.DecimalFormat;
import java.util.List;
import java.util.function.Function;

public class StringDedupLast implements PageCreator {
    @Override
    public Page create(JvmLogFile jvmLogFile, DecimalFormat decimalFormat) {
        return Page.builder()
                .menuName("Str. Dedup. exec stats")
                .fullName("String Deduplication execution stats")
                .info("Following charts show how much memory your application saves due to String Deduplication on each run")
                .icon(Page.Icon.CHART)
                .pageContents(
                        List.of(
                                Chart.builder()
                                        .chartType(Chart.ChartType.LINE)
                                        .title("Size of deduplicated strings (in kB) in one execution")
                                        .xAxisLabel("Seconds since application start")
                                        .yAxisLabel("Size of strings deduplicated during event (in kB)")
                                        .data(getSizeChart(jvmLogFile.getStringDedupLogFile().getEntries()))
                                        .build(),
                                Chart.builder()
                                        .chartType(Chart.ChartType.LINE)
                                        .title("Count of deduplicated strings in one execution")
                                        .xAxisLabel("Seconds since application start")
                                        .yAxisLabel("Number of strings deduplicated during event")
                                        .data(getCountChart(jvmLogFile.getStringDedupLogFile().getEntries()))
                                        .build(),
                                Chart.builder()
                                        .chartType(Chart.ChartType.LINE)
                                        .title("Size (in Kb) of scanned strings in one execution")
                                        .xAxisLabel("Seconds since application start")
                                        .yAxisLabel("Size of strings scanned for potential deduplication during event (in kB)")
                                        .data(getNewSizeChart(jvmLogFile.getStringDedupLogFile().getEntries()))
                                        .build(),
                                Chart.builder()
                                        .chartType(Chart.ChartType.LINE)
                                        .title("Count of scanned strings in one execution")
                                        .xAxisLabel("Seconds since application start")
                                        .yAxisLabel("Number of strings scanned for potential deduplication during event")
                                        .data(getNewCountChart(jvmLogFile.getStringDedupLogFile().getEntries()))
                                        .build()
                        )
                )
                .build();
    }

    private static final List<String> sizeChartColumns = List.of(
            "Time",
            "Total size",
            "Young size",
            "Old size");
    private static final List<Function<StringDedupLogEntry, Object>> sizeChartExtractors = List.of(
            StringDedupLogEntry::getTimeStamp,
            StringDedupLogEntry::getLastSize,
            StringDedupLogEntry::getLastSizeYoung,
            StringDedupLogEntry::getLastSizeOld);

    private static Object[][] getSizeChart(List<StringDedupLogEntry> entries) {
        return PageUtils.toMatrix(entries, sizeChartColumns, sizeChartExtractors);
    }

    private static final List<String> countChartColumns = List.of(
            "Time",
            "Last count",
            "Young count",
            "Old count");
    private static final List<Function<StringDedupLogEntry, Object>> countChartExtractors = List.of(
            StringDedupLogEntry::getTimeStamp,
            StringDedupLogEntry::getLastCount,
            StringDedupLogEntry::getLastCountYoung,
            StringDedupLogEntry::getLastCountOld);

    private static Object[][] getCountChart(List<StringDedupLogEntry> entries) {
        return PageUtils.toMatrix(entries, countChartColumns, countChartExtractors);
    }

    private static final List<String> newCountChartColumns = List.of(
            "Time",
            "New strings count");
    private static final List<Function<StringDedupLogEntry, Object>> newCountChartExtractors = List.of(
            StringDedupLogEntry::getTimeStamp,
            StringDedupLogEntry::getLastCountNew);

    private static Object[][] getNewCountChart(List<StringDedupLogEntry> entries) {
        return PageUtils.toMatrix(entries, newCountChartColumns, newCountChartExtractors);
    }

    private static final List<String> newSizeChartColumns = List.of(
            "Time",
            "New strings size");
    private static final List<Function<StringDedupLogEntry, Object>> newSizeChartExtractors = List.of(
            StringDedupLogEntry::getTimeStamp,
            StringDedupLogEntry::getLastSizeNew);

    private static Object[][] getNewSizeChart(List<StringDedupLogEntry> entries) {
        return PageUtils.toMatrix(entries, newSizeChartColumns, newSizeChartExtractors);
    }
}
