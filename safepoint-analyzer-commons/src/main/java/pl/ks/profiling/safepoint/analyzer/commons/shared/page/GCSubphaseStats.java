package pl.ks.profiling.safepoint.analyzer.commons.shared.page;

import static pl.ks.profiling.gui.commons.PageCreatorHelper.numToString;

import java.text.DecimalFormat;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import pl.ks.profiling.gui.commons.Page;
import pl.ks.profiling.gui.commons.PageContent;
import pl.ks.profiling.gui.commons.Table;
import pl.ks.profiling.safepoint.analyzer.commons.shared.pareser.safepoint.SafepointLogFile;

public class GCSubphaseStats implements PageCreator{
    @Override
    public Page create(SafepointLogFile safepointLogFile, DecimalFormat decimalFormat) {
        List<PageContent> pageContents = safepointLogFile.getGcStats().getGcAggregatedPhaseStats().stream()
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
