package pl.ks.profiling.safepoint.analyzer.commons.shared.safepoint.page;

import static pl.ks.profiling.gui.commons.PageCreatorHelper.numToString;

import java.text.DecimalFormat;
import java.util.List;
import java.util.stream.Collectors;
import pl.ks.profiling.gui.commons.Page;
import pl.ks.profiling.gui.commons.Table;
import pl.ks.profiling.safepoint.analyzer.commons.shared.JvmLogFile;
import pl.ks.profiling.safepoint.analyzer.commons.shared.PageCreator;
import pl.ks.profiling.safepoint.analyzer.commons.shared.safepoint.parser.SafepointOperationStats;

public class SafepointTableStats implements PageCreator {
    @Override
    public Page create(JvmLogFile jvmLogFile, DecimalFormat decimalFormat) {
        SafepointOperationStats soStats = jvmLogFile.getSafepointLogFile().getSafepointOperationStats();
        return Page.builder()
                .menuName("Safepoint table stats")
                .fullName("Safepoint operation statistics")
                .icon(Page.Icon.STATS)
                .pageContents(List.of(
                        Table.builder()
                                .header(List.of("Total count"))
                                .info("Table presents number of Stop The World phases in your JVM.")
                                .table(List.of(List.of(soStats.getTotalCount() + "")))
                                .screenWidth("25%")
                                .build(),
                        Table.builder()
                                .header(List.of("Percentile", "Time to safepoint time", "Safepoint operation time", "Application time"))
                                .info("Application time - time that your application is really running, Safepoint operation time - this is the time of Stop The World phase, Time to safepoint time - wasted time between JVM ordered Stop The World phase and real start of following phase.")
                                .table(List.of(
                                        List.of("50", numToString(soStats.getTts().getPercentile50(), decimalFormat), numToString(soStats.getOperationTime().getPercentile50(), decimalFormat), numToString(soStats.getApplicationTime().getPercentile50(), decimalFormat)),
                                        List.of("75", numToString(soStats.getTts().getPercentile75(), decimalFormat), numToString(soStats.getOperationTime().getPercentile75(), decimalFormat), numToString(soStats.getApplicationTime().getPercentile75(), decimalFormat)),
                                        List.of("90", numToString(soStats.getTts().getPercentile90(), decimalFormat), numToString(soStats.getOperationTime().getPercentile90(), decimalFormat), numToString(soStats.getApplicationTime().getPercentile90(), decimalFormat)),
                                        List.of("95", numToString(soStats.getTts().getPercentile95(), decimalFormat), numToString(soStats.getOperationTime().getPercentile95(), decimalFormat), numToString(soStats.getApplicationTime().getPercentile95(), decimalFormat)),
                                        List.of("99", numToString(soStats.getTts().getPercentile99(), decimalFormat), numToString(soStats.getOperationTime().getPercentile99(), decimalFormat), numToString(soStats.getApplicationTime().getPercentile99(), decimalFormat)),
                                        List.of("99.9", numToString(soStats.getTts().getPercentile99and9(), decimalFormat), numToString(soStats.getOperationTime().getPercentile99and9(), decimalFormat), numToString(soStats.getApplicationTime().getPercentile99and9(), decimalFormat)),
                                        List.of("100", numToString(soStats.getTts().getPercentile100(), decimalFormat), numToString(soStats.getOperationTime().getPercentile100(), decimalFormat), numToString(soStats.getApplicationTime().getPercentile100(), decimalFormat)),
                                        List.of("Average", numToString(soStats.getTts().getAverage(), decimalFormat), numToString(soStats.getOperationTime().getAverage(), decimalFormat), numToString(soStats.getApplicationTime().getAverage(), decimalFormat)),
                                        List.of("Total", numToString(soStats.getTts().getTotal(), decimalFormat), numToString(soStats.getOperationTime().getTotal(), decimalFormat), numToString(soStats.getApplicationTime().getTotal(), decimalFormat))
                                ))
                                .build(),
                        Table.builder()
                                .header(List.of("Safepoint operation name", "Count", "Per. 50", "Per. 75", "Per. 90", "Per. 95", "Per. 99", "Per. 99.9", "Per. 100", "Average", "Total"))
                                .info("Table presents what Safepoint operationt caused Stop The World phase.")
                                .table(
                                        soStats.getStatsByNames().stream()
                                                .map(stat -> List.of(stat.getOperationName(),
                                                        stat.getCount() + "",
                                                        numToString(stat.getOperationTime().getPercentile50(), decimalFormat),
                                                        numToString(stat.getOperationTime().getPercentile75(), decimalFormat),
                                                        numToString(stat.getOperationTime().getPercentile90(), decimalFormat),
                                                        numToString(stat.getOperationTime().getPercentile95(), decimalFormat),
                                                        numToString(stat.getOperationTime().getPercentile99(), decimalFormat),
                                                        numToString(stat.getOperationTime().getPercentile99and9(), decimalFormat),
                                                        numToString(stat.getOperationTime().getPercentile100(), decimalFormat),
                                                        numToString(stat.getOperationTime().getAverage(), decimalFormat),
                                                        numToString(stat.getOperationTime().getTotal(), decimalFormat)
                                                ))
                                                .collect(Collectors.toList())
                                )
                                .build()

                ))
                .build();
    }
}
