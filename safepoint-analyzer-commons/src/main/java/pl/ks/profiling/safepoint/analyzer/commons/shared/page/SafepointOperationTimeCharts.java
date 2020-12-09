package pl.ks.profiling.safepoint.analyzer.commons.shared.page;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import pl.ks.profiling.gui.commons.Chart;
import pl.ks.profiling.gui.commons.Page;
import pl.ks.profiling.safepoint.analyzer.commons.shared.parser.JvmLogFile;
import pl.ks.profiling.safepoint.analyzer.commons.shared.parser.safepoint.SafepointLogEntry;
import pl.ks.profiling.safepoint.analyzer.commons.shared.parser.safepoint.SafepointOperationStatsByName;

public class SafepointOperationTimeCharts implements PageCreator{
    public static final BigDecimal MULTIPLIER = new BigDecimal(1000);

    @Override
    public Page create(JvmLogFile jvmLogFile, DecimalFormat decimalFormat) {
        return Page.builder()
                .menuName("SO time")
                .fullName("Safepoint operation time with TTS")
                .icon(Page.Icon.CHART)
                .pageContents(List.of(
                        Chart.builder()
                                .info("Total time that your JVM wasted on Stop The World phases on chart.")
                                .title("Time in STW phases with TTS")
                                .chartType(Chart.ChartType.POINTS)
                                .data(getChart(jvmLogFile))
                                .build()
                ))
                .build();
    }

    private static Object[][] getChart(JvmLogFile jvmLogFile) {
        List<String> names = jvmLogFile.getSafepointLogFile().getSafepointOperationStats().getStatsByNames().stream()
                .map(SafepointOperationStatsByName::getOperationName)
                .collect(Collectors.toList());
        Object[][] stats = new Object[jvmLogFile.getSafepointLogFile().getSafepoints().size() + 1][names.size() + 1];

        Map<String, Integer> namesMap = new HashMap<>();
        int j = 1;
        for (String name : names) {
            namesMap.put(name, j);
            stats[0][j] = name;
            j++;
        }

        stats[0][0] = "Time";
        int i = 1;
        for (SafepointLogEntry safepointLogEntry : jvmLogFile.getSafepointLogFile().getSafepoints()) {
            stats[i][0] = safepointLogEntry.getTimeStamp();
            stats[i][namesMap.get(safepointLogEntry.getOperationName())] = safepointLogEntry.getStoppedTime().add(safepointLogEntry.getTtsTime()).multiply(MULTIPLIER);
            i++;
        }
        return stats;
    }
}
