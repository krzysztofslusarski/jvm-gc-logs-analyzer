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

import static pl.ks.profiling.gui.commons.PageCreatorHelper.numToString;

import java.text.DecimalFormat;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import pl.ks.profiling.gui.commons.Page;
import pl.ks.profiling.gui.commons.PageContent;
import pl.ks.profiling.gui.commons.Table;
import pl.ks.profiling.safepoint.analyzer.commons.shared.JvmLogFile;
import pl.ks.profiling.safepoint.analyzer.commons.shared.PageCreator;

public class GCSubphaseStats implements PageCreator {
    @Override
    public Page create(JvmLogFile jvmLogFile, DecimalFormat decimalFormat) {
        List<PageContent> pageContents = jvmLogFile.getGcLogFile().getStats().getGcAggregatedPhaseStats().stream()
                .map(stat -> Table.builder()
                        .header(List.of("Subphase name", "Per. 50", "Per. 75", "Per. 90", "Per. 95", "Per. 99", "Per. 99.9", "Per. 100", "Average", "Total"))
                        .title(stat.getName() + " - subphase stats - times in ms")
                        .table(stat.getSubPhaseTimes().entrySet().stream()
                                .filter(Objects::nonNull)
                                .filter(entry -> entry.getKey() != null && entry.getValue() != null && entry.getValue().getTotal() != null)
                                .map(entry -> List.of(
                                        entry.getKey(),
                                        numToString(entry.getValue().getPercentile50(), decimalFormat),
                                        numToString(entry.getValue().getPercentile75(), decimalFormat),
                                        numToString(entry.getValue().getPercentile90(), decimalFormat),
                                        numToString(entry.getValue().getPercentile95(), decimalFormat),
                                        numToString(entry.getValue().getPercentile99(), decimalFormat),
                                        numToString(entry.getValue().getPercentile99and9(), decimalFormat),
                                        numToString(entry.getValue().getPercentile100(), decimalFormat),
                                        numToString(entry.getValue().getAverage(), decimalFormat),
                                        numToString(entry.getValue().getTotal(), decimalFormat)
                                ))
                                .collect(Collectors.toList()))
                        .build())
                .filter(table -> table.getTable().size() > 0)
                .collect(Collectors.toList());
        if (pageContents.size() == 0) {
            return null;
        }

        return Page.builder()
                .menuName("GC agr. subphase time")
                .fullName("Garbage Collector aggregated subphase time")
                .info("Tables presntes statistics of subphases of each Garbage Collectore phase. More detailed info are available after adding gc+phases=trace to your Xlog.")
                .icon(Page.Icon.STATS)
                .pageContents(pageContents)
                .build();
    }
}
