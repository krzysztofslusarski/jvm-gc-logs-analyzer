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
package pl.ks.profiling.safepoint.analyzer.commons.shared.safepoint.page;

import pl.ks.profiling.gui.commons.Chart;
import pl.ks.profiling.gui.commons.Page;
import pl.ks.profiling.safepoint.analyzer.commons.shared.JvmLogFile;
import pl.ks.profiling.safepoint.analyzer.commons.shared.PageCreator;
import pl.ks.profiling.safepoint.analyzer.commons.shared.PageUtils;
import pl.ks.profiling.safepoint.analyzer.commons.shared.safepoint.parser.SafepointOperationStats;
import pl.ks.profiling.safepoint.analyzer.commons.shared.safepoint.parser.SafepointOperationStatsByName;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.util.List;
import java.util.function.Function;

public class SafepoinOperationTime implements PageCreator {
    @Override
    public Page create(JvmLogFile jvmLogFile, DecimalFormat decimalFormat) {
        SafepointOperationStats soStats = jvmLogFile.getSafepointLogFile().getSafepointOperationStats();
        return Page.builder()
                .menuName("Safepoint operation time")
                .fullName("Safepoint operation time + avg(TTS)")
                .icon(Page.Icon.CHART)
                .pageContents(List.of(
                        Chart.builder()
                                .title("Safepoint operation time + avg(TTS)")
                                .info("Total time that your JVM wasted on Stop-the-world phases splited by Safepoint operation that caused it.")
                                .chartType(Chart.ChartType.PIE)
                                .data(getChart(soStats))
                                .build()
                ))
                .build();
    }

    private static final List<String> chartColumns = List.of(
            "Operation name",
            "Time with TTS");

    private static Object[][] getChart(SafepointOperationStats safepointOperationStats) {
        Function<SafepointOperationStatsByName, Object> getTimeWithTts = (SafepointOperationStatsByName statByName) ->
                statByName.getOperationTime().getTotal()
                        .add(safepointOperationStats.getTts().getAverage().multiply(new BigDecimal(statByName.getCount())));

        List<Function<SafepointOperationStatsByName, Object>> chartExtractors = List.of(
                SafepointOperationStatsByName::getOperationName,
                getTimeWithTts);
        return PageUtils.toMatrix(safepointOperationStats.getStatsByNames(), chartColumns, chartExtractors);
    }
}
