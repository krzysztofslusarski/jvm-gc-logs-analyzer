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
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import pl.ks.profiling.gui.commons.Page;
import pl.ks.profiling.gui.commons.PageContent;
import pl.ks.profiling.gui.commons.Table;
import pl.ks.profiling.safepoint.analyzer.commons.shared.report.JvmLogFile;
import pl.ks.profiling.safepoint.analyzer.commons.shared.OneFiledAllStats;
import pl.ks.profiling.safepoint.analyzer.commons.shared.PageCreator;
import pl.ks.profiling.safepoint.analyzer.commons.shared.gc.parser.GCStats;

public class GCTableStats implements PageCreator {
    @Override
    public Page create(JvmLogFile jvmLogFile, DecimalFormat decimalFormat) {
        List<PageContent> pageContents = new ArrayList<>();
        GCStats gcStats = jvmLogFile.getGcLogFile().getStats();
        if (gcStats.getMaxSurvivorAge() > 0) {
            pageContents.add(Table.builder()
                    .header(List.of("Age promotion", "Survival ratio"))
                    .title("Survivals ratio")
                    .info("Table presents what is the average of survivors in each age.")
                    .screenWidth("25%")
                    .table(IntStream.range(1, (int) (gcStats.getMaxSurvivorAge()))
                            .mapToObj(i -> List.of(
                                    i + " -> " + (i + 1),
                                    numToString(gcStats.getGcAgingSummary().getSurvivedRatio().get(i) * 100, decimalFormat) + "%"))
                            .collect(Collectors.toList()))
                    .build());
        }
        if (gcStats.getGcAgingSummary().getAgingSizes().size() > 0) {
            pageContents.add(Table.builder()
                    .header(List.of("Age", "Per. 50", "Per. 75", "Per. 90", "Per. 95", "Per. 99", "Per. 99.9", "Per. 100", "Average"))
                    .title("Survivals size")
                    .info("Table presents statistics about survivor sizes in each age (size in bytes).")
                    .table(gcStats.getGcAgingSummary().getAgingSizes().entrySet().stream()
                            .map(entry -> List.of(entry.getKey() + "",
                                    numToString(entry.getValue().getPercentile50(), decimalFormat),
                                    numToString(entry.getValue().getPercentile75(), decimalFormat),
                                    numToString(entry.getValue().getPercentile90(), decimalFormat),
                                    numToString(entry.getValue().getPercentile95(), decimalFormat),
                                    numToString(entry.getValue().getPercentile99(), decimalFormat),
                                    numToString(entry.getValue().getPercentile99and9(), decimalFormat),
                                    numToString(entry.getValue().getPercentile100(), decimalFormat),
                                    numToString(entry.getValue().getAverage(), decimalFormat)
                            ))
                            .collect(Collectors.toList())
                    )
                    .build());
        }
        if (gcStats.getGcAggregatedPhaseStats().size() < gcStats.getGcPhaseStats().size()) {
            pageContents.add(Table.builder()
                    .header(List.of("Phase name", "Count", "Per. 50", "Per. 75", "Per. 90", "Per. 95", "Per. 99", "Per. 99.9", "Per. 100", "Average", "Total"))
                    .title("Phase stats (aggregated) - times in ms")
                    .info("Table presents statistics about each Stop-the-world Garbage Collector phase. Phases are aggregated to major type of collection.")
                    .table(gcStats.getGcAggregatedPhaseStats().stream()
                            .map(stat -> List.of(
                                    stat.getName(),
                                    stat.getCount() + "",
                                    numToString(stat.getTime().getPercentile50(), decimalFormat),
                                    numToString(stat.getTime().getPercentile75(), decimalFormat),
                                    numToString(stat.getTime().getPercentile90(), decimalFormat),
                                    numToString(stat.getTime().getPercentile95(), decimalFormat),
                                    numToString(stat.getTime().getPercentile99(), decimalFormat),
                                    numToString(stat.getTime().getPercentile99and9(), decimalFormat),
                                    numToString(stat.getTime().getPercentile100(), decimalFormat),
                                    numToString(stat.getTime().getAverage(), decimalFormat),
                                    numToString(stat.getTime().getTotal(), decimalFormat)
                            ))
                            .collect(Collectors.toList())
                    )
                    .build());
        }
        pageContents.add(Table.builder()
                .header(List.of("Phase name", "Count", "Per. 50", "Per. 75", "Per. 90", "Per. 95", "Per. 99", "Per. 99.9", "Per. 100", "Average", "Total"))
                .title("Phase stats - times in ms")
                .info("Table presents statistics about each Stop-the-world Garbage Collector phase without aggregation.")
                .table(gcStats.getGcPhaseStats().stream()
                        .map(stat -> List.of(
                                stat.getName(),
                                stat.getCount() + "",
                                numToString(stat.getTime().getPercentile50(), decimalFormat),
                                numToString(stat.getTime().getPercentile75(), decimalFormat),
                                numToString(stat.getTime().getPercentile90(), decimalFormat),
                                numToString(stat.getTime().getPercentile95(), decimalFormat),
                                numToString(stat.getTime().getPercentile99(), decimalFormat),
                                numToString(stat.getTime().getPercentile99and9(), decimalFormat),
                                numToString(stat.getTime().getPercentile100(), decimalFormat),
                                numToString(stat.getTime().getAverage(), decimalFormat),
                                numToString(stat.getTime().getTotal(), decimalFormat)
                        ))
                        .collect(Collectors.toList())
                )
                .build());
        if (gcStats.getGcConcurrentCycleStats().size() > 0) {
            pageContents.add(Table.builder()
                    .header(List.of("Concurrent phase name", "Count", "Per. 50", "Per. 75", "Per. 90", "Per. 95", "Per. 99", "Per. 99.9", "Per. 100", "Average", "Total"))
                    .title("Concurrent stats - times in ms")
                    .info("Table presents statistics about Concurrent Cycle of Garbage Collector.")
                    .table(gcStats.getGcConcurrentCycleStats().stream()
                            .map(stat -> List.of(
                                    stat.getName(),
                                    stat.getCount() + "",
                                    numToString(stat.getTime().getPercentile50(), decimalFormat),
                                    numToString(stat.getTime().getPercentile75(), decimalFormat),
                                    numToString(stat.getTime().getPercentile90(), decimalFormat),
                                    numToString(stat.getTime().getPercentile95(), decimalFormat),
                                    numToString(stat.getTime().getPercentile99(), decimalFormat),
                                    numToString(stat.getTime().getPercentile99and9(), decimalFormat),
                                    numToString(stat.getTime().getPercentile100(), decimalFormat),
                                    numToString(stat.getTime().getAverage(), decimalFormat),
                                    numToString(stat.getTime().getTotal(), decimalFormat)
                            ))
                            .collect(Collectors.toList())
                    )
                    .build());
        }
        OneFiledAllStats allHumongousStats = gcStats.getAllHumongousStats();
        OneFiledAllStats liveHumongousStats = gcStats.getLiveHumongousStats();
        OneFiledAllStats deadHumongousStats = gcStats.getDeadHumongousStats();

        List<List<String>> table = new ArrayList<>();
        if (liveHumongousStats != null) {
            table.add(List.of("Live",
                    numToString(liveHumongousStats.getCount(), decimalFormat),
                    numToString(liveHumongousStats.getPercentile50(), decimalFormat),
                    numToString(liveHumongousStats.getPercentile75(), decimalFormat),
                    numToString(liveHumongousStats.getPercentile90(), decimalFormat),
                    numToString(liveHumongousStats.getPercentile95(), decimalFormat),
                    numToString(liveHumongousStats.getPercentile99(), decimalFormat),
                    numToString(liveHumongousStats.getPercentile99and9(), decimalFormat),
                    numToString(liveHumongousStats.getPercentile100(), decimalFormat),
                    numToString(liveHumongousStats.getAverage(), decimalFormat)
            ));
        }
        if (deadHumongousStats != null) {
            table.add(List.of("Dead",
                    numToString(deadHumongousStats.getCount(), decimalFormat),
                    numToString(deadHumongousStats.getPercentile50(), decimalFormat),
                    numToString(deadHumongousStats.getPercentile75(), decimalFormat),
                    numToString(deadHumongousStats.getPercentile90(), decimalFormat),
                    numToString(deadHumongousStats.getPercentile95(), decimalFormat),
                    numToString(deadHumongousStats.getPercentile99(), decimalFormat),
                    numToString(deadHumongousStats.getPercentile99and9(), decimalFormat),
                    numToString(deadHumongousStats.getPercentile100(), decimalFormat),
                    numToString(deadHumongousStats.getAverage(), decimalFormat)
            ));
        }

        if (allHumongousStats != null) {
            table.add(List.of("All (Live + Dead)",
                    numToString(allHumongousStats.getCount(), decimalFormat),
                    numToString(allHumongousStats.getPercentile50(), decimalFormat),
                    numToString(allHumongousStats.getPercentile75(), decimalFormat),
                    numToString(allHumongousStats.getPercentile90(), decimalFormat),
                    numToString(allHumongousStats.getPercentile95(), decimalFormat),
                    numToString(allHumongousStats.getPercentile99(), decimalFormat),
                    numToString(allHumongousStats.getPercentile99and9(), decimalFormat),
                    numToString(allHumongousStats.getPercentile100(), decimalFormat),
                    numToString(allHumongousStats.getAverage(), decimalFormat)
            ));
        }

        if (!table.isEmpty()) {
            pageContents.add(Table.builder()
                    .header(List.of("Type", "Count", "Per. 50", "Per. 75", "Per. 90", "Per. 95", "Per. 99", "Per. 99.9", "Per. 100", "Average"))
                    .title("Humongous statistics")
                    .info("Table presents statistics about humongous regions (sizes in bytes)")
                    .table(table)
                    .build());
        }

        if (gcStats.getFullGcSequenceIds().size() > 0) {
            List<String> header = new ArrayList<>();
            header.add("Cycle");
            pageContents.add(Table.builder()
                    .header(header)
                    .title("Full GC cycle ids")
                    .info("List of Full GC cycles")
                    .table(gcStats.getFullGcSequenceIds().stream()
                            .map(id -> List.of(id.toString()))
                            .collect(Collectors.toList()))
                    .build());
        }

        if (gcStats.getToSpaceStats().size() > 0) {
            List<String> header = new ArrayList<>();
            header.add("Cycle");
            header.addAll(gcStats.getGcRegions());
            pageContents.add(Table.builder()
                    .header(header)
                    .title("To-space exhausted")
                    .info("List of GC cycles where To-space exhausted occured")
                    .table(gcStats.getToSpaceStats().stream()
                            .map(gcToSpaceStats -> {
                                List<String> stats = new ArrayList<>();
                                stats.add(String.valueOf(gcToSpaceStats.getSequenceId()));
                                for (String region : gcStats.getGcRegions()) {
                                    stats.add(gcToSpaceStats.getRegionStats().get(region));
                                }
                                return stats;
                            })
                            .collect(Collectors.toList()))
                    .build());
        }

        return Page.builder()
                .menuName("GC table stats")
                .fullName("Garbage Collector table stats")
                .icon(Page.Icon.STATS)
                .pageContents(pageContents)
                .build();
    }
}
