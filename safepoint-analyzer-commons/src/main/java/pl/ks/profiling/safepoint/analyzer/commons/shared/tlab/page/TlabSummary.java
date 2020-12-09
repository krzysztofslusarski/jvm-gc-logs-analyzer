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
package pl.ks.profiling.safepoint.analyzer.commons.shared.tlab.page;

import java.text.DecimalFormat;
import java.util.List;
import pl.ks.profiling.gui.commons.Chart;
import pl.ks.profiling.gui.commons.Page;
import pl.ks.profiling.safepoint.analyzer.commons.shared.JvmLogFile;
import pl.ks.profiling.safepoint.analyzer.commons.shared.PageCreator;
import pl.ks.profiling.safepoint.analyzer.commons.shared.tlab.parser.TlabSummaryInfo;

public class TlabSummary implements PageCreator {
    @Override
    public Page create(JvmLogFile jvmLogFile, DecimalFormat decimalFormat) {
        return Page.builder()
                .menuName("TLAB summary")
                .fullName("TLAB summary")
                .info("Following charts shows TLAB summery.")
                .icon(Page.Icon.CHART)
                .pageContents(
                        List.of(
                                Chart.builder()
                                        .chartType(Chart.ChartType.POINTS)
                                        .title("Slow allocation of objects")
                                        .data(getSlowAllocationChart(jvmLogFile.getTlabLogFile().getTlabSummaries()))
                                        .build(),
                                Chart.builder()
                                        .chartType(Chart.ChartType.POINTS)
                                        .title("TLAB refills")
                                        .data(getRefillsChart(jvmLogFile.getTlabLogFile().getTlabSummaries()))
                                        .build(),
                                Chart.builder()
                                        .chartType(Chart.ChartType.POINTS)
                                        .title("Waste percent")
                                        .data(getWasteChart(jvmLogFile.getTlabLogFile().getTlabSummaries()))
                                        .build(),
                                Chart.builder()
                                        .chartType(Chart.ChartType.POINTS)
                                        .title("Number of allocating threads")
                                        .data(getAllocatingThreadsChart(jvmLogFile.getTlabLogFile().getTlabSummaries()))
                                        .build()
                        )
                )
                .build();
    }

    private static Object[][] getAllocatingThreadsChart(List<TlabSummaryInfo> tlabSummaries) {
        Object[][] stats = new Object[tlabSummaries.size() + 1][2];
        stats[0][0] = "Time";
        stats[0][1] = "Allocation threads";
        int i = 1;
        for (TlabSummaryInfo tlabSummary : tlabSummaries) {
            stats[i][0] = tlabSummary.getTimeStamp();
            stats[i][1] = tlabSummary.getThreadCount();
            i++;
        }
        return stats;
    }

    private static Object[][] getSlowAllocationChart(List<TlabSummaryInfo> tlabSummaries) {
        Object[][] stats = new Object[tlabSummaries.size() + 1][3];
        stats[0][0] = "Time";
        stats[0][1] = "Slow allocations";
        stats[0][2] = "One thread max slow allocations";
        int i = 1;
        for (TlabSummaryInfo tlabSummary : tlabSummaries) {
            stats[i][0] = tlabSummary.getTimeStamp();
            stats[i][1] = tlabSummary.getSlowAllocs();
            stats[i][2] = tlabSummary.getMaxSlowAllocs();
            i++;
        }
        return stats;
    }

    private static Object[][] getRefillsChart(List<TlabSummaryInfo> tlabSummaries) {
        Object[][] stats = new Object[tlabSummaries.size() + 1][3];
        stats[0][0] = "Time";
        stats[0][1] = "Refills";
        stats[0][2] = "One thread max refills";
        int i = 1;
        for (TlabSummaryInfo tlabSummary : tlabSummaries) {
            stats[i][0] = tlabSummary.getTimeStamp();
            stats[i][1] = tlabSummary.getRefills();
            stats[i][2] = tlabSummary.getMaxRefills();
            i++;
        }
        return stats;
    }

    private static Object[][] getWasteChart(List<TlabSummaryInfo> tlabSummaries) {
        Object[][] stats = new Object[tlabSummaries.size() + 1][2];
        stats[0][0] = "Time";
        stats[0][1] = "Waste %";
        int i = 1;
        for (TlabSummaryInfo tlabSummary : tlabSummaries) {
            stats[i][0] = tlabSummary.getTimeStamp();
            stats[i][1] = tlabSummary.getWastePercent();
            i++;
        }
        return stats;
    }
}
