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
package pl.ks.profiling.safepoint.analyzer.commons.shared.safepoint.page;

import java.text.DecimalFormat;
import java.util.List;
import java.util.Set;
import pl.ks.profiling.gui.commons.Chart;
import pl.ks.profiling.gui.commons.Page;
import pl.ks.profiling.safepoint.analyzer.commons.shared.report.JvmLogFile;
import pl.ks.profiling.safepoint.analyzer.commons.shared.PageCreator;
import pl.ks.profiling.safepoint.analyzer.commons.shared.safepoint.parser.SafepointOperationStats;
import pl.ks.profiling.safepoint.analyzer.commons.shared.safepoint.parser.SafepointOperationStatsByName;

public class SafepointTotalTimeInPhases implements PageCreator {
    @Override
    public Page create(JvmLogFile jvmLogFile, DecimalFormat decimalFormat) {
        SafepointOperationStats soStats = jvmLogFile.getSafepointLogFile().getSafepointOperationStats();
        return Page.builder()
                .menuName("Total time in phases")
                .fullName("Total time in phases")
                .icon(Page.Icon.CHART)
                .pageContents(List.of(
                        Chart.builder()
                                .chartType(Chart.ChartType.PIE)
                                .title("Total time in phases")
                                .info("Application time - time that your application is really running, Safepoint operation time - this is the time of Stop-the-world phase, Time to safepoint time - wasted time between JVM ordered Stop-the-world phase and real start of following phase.")
                                .data(getChart(soStats))
                                .build()
                ))
                .build();
    }

    private static Object[][] getChart(SafepointOperationStats safepointOperationStats) {
        Set<SafepointOperationStatsByName> statsByName = safepointOperationStats.getStatsByNames();
        Object[][] stats = new Object[4][2];
        stats[0][0] = "Phase name";
        stats[0][1] = "Total time";
        stats[1][0] = "Application time";
        stats[2][0] = "Safepoint operation time";
        stats[3][0] = "Time to safepoint time";
        stats[1][1] = safepointOperationStats.getApplicationTime().getTotal();
        stats[2][1] = safepointOperationStats.getOperationTime().getTotal();
        stats[3][1] = safepointOperationStats.getTts().getTotal();
        return stats;
    }
}
