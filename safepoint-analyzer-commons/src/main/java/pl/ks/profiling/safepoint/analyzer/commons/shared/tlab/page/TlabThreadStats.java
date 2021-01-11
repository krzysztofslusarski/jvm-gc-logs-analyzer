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

import static pl.ks.profiling.gui.commons.PageCreatorHelper.numToString;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import pl.ks.profiling.gui.commons.Page;
import pl.ks.profiling.gui.commons.Table;
import pl.ks.profiling.safepoint.analyzer.commons.shared.report.JvmLogFile;
import pl.ks.profiling.safepoint.analyzer.commons.shared.PageCreator;

public class TlabThreadStats implements PageCreator {
    @Override
    public Page create(JvmLogFile jvmLogFile, DecimalFormat decimalFormat) {
        return Page.builder()
                .menuName("TLAB thread stats")
                .fullName("TLAB thread statistics")
                .info("Following table shows TLAB stats per thread.")
                .icon(Page.Icon.CHART)
                .pageContents(
                        List.of(
                                Table.builder()
                                        .header(List.of("Tid", "Nid", "Per. 50", "Per. 99", "Per. 99.9", "Per. 100", "Average", "Max size (KB)", "Avg size (KB)", "Total", "Count"))
                                        .title("Slow allocations")
                                        .info("Table presents slow allocation (in eden) per thread.")
                                        .table(jvmLogFile.getTlabLogFile().getThreadTlabStats().stream()
                                                .filter(threadTlabInfo -> threadTlabInfo.getSlowAllocs().getTotal().compareTo(BigDecimal.ZERO) > 0)
                                                .sorted(Comparator.comparing(value -> -value.getSlowAllocs().getTotal().doubleValue()))
                                                .map(threadTlabInfo -> List.of(threadTlabInfo.getTid() + "",
                                                        threadTlabInfo.getNid() + "",
                                                        numToString(threadTlabInfo.getSlowAllocs().getPercentile50(), decimalFormat),
                                                        numToString(threadTlabInfo.getSlowAllocs().getPercentile99(), decimalFormat),
                                                        numToString(threadTlabInfo.getSlowAllocs().getPercentile99and9(), decimalFormat),
                                                        numToString(threadTlabInfo.getSlowAllocs().getPercentile100(), decimalFormat),
                                                        numToString(threadTlabInfo.getSlowAllocs().getAverage(), decimalFormat),
                                                        numToString(threadTlabInfo.getSize().getPercentile100(), decimalFormat),
                                                        numToString(threadTlabInfo.getSize().getAverage(), decimalFormat),
                                                        numToString(threadTlabInfo.getSlowAllocs().getTotal(), decimalFormat),
                                                        numToString(threadTlabInfo.getSlowAllocs().getCount(), decimalFormat)
                                                ))
                                                .collect(Collectors.toList())
                                        )
                                        .build()
                        )
                )
                .build();
    }
}
