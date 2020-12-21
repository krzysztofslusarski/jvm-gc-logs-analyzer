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

import java.text.DecimalFormat;
import java.util.List;
import java.util.function.Function;

import pl.ks.profiling.gui.commons.Chart;
import pl.ks.profiling.gui.commons.Page;
import pl.ks.profiling.safepoint.analyzer.commons.shared.JvmLogFile;
import pl.ks.profiling.safepoint.analyzer.commons.shared.PageCreator;
import pl.ks.profiling.safepoint.analyzer.commons.shared.PageUtils;
import pl.ks.profiling.safepoint.analyzer.commons.shared.safepoint.parser.SafepointOperationStats;
import pl.ks.profiling.safepoint.analyzer.commons.shared.safepoint.parser.TimesInTime;

public class SafepointApplicationTimeByTime5Sec implements PageCreator {
    @Override
    public Page create(JvmLogFile jvmLogFile, DecimalFormat decimalFormat) {
        SafepointOperationStats soStats = jvmLogFile.getSafepointLogFile().getSafepointOperationStats();
        return Page.builder()
                .menuName("Application time (in time) - 5s")
                .fullName("Application time (in time) - 5s window")
                .info("Following charts shows time when your application was running on your JVM in different window time.")
                .icon(Page.Icon.CHART)
                .pageContents(
                        List.of(
                                Chart.builder()
                                        .forceZeroMinValue(true)
                                        .chartType(Chart.ChartType.LINE)
                                        .title("5 second window")
                                        .data(getChart(soStats.getTimesInTimes5sec()))
                                        .build()
                        )
                )
                .build();
    }

    private static final List<String> chartColumns = List.of(
            "Phase start time",
            "Application time");
    private static final List<Function<TimesInTime, Object>> chartExtractors = List.of(
            TimesInTime::getStartTime,
            TimesInTime::getApplicationTime);

    private static Object[][] getChart(List<TimesInTime> timesInTimes) {
        return PageUtils.toMatrix(timesInTimes, chartColumns, chartExtractors);
    }
}
