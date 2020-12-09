package pl.ks.profiling.safepoint.analyzer.commons.shared.stringdedup.page;

import java.text.DecimalFormat;
import java.util.List;
import pl.ks.profiling.gui.commons.Chart;
import pl.ks.profiling.gui.commons.Page;
import pl.ks.profiling.safepoint.analyzer.commons.shared.JvmLogFile;
import pl.ks.profiling.safepoint.analyzer.commons.shared.PageCreator;
import pl.ks.profiling.safepoint.analyzer.commons.shared.stringdedup.parser.StringDedupLogEntry;

public class StringDedupLast implements PageCreator {
    @Override
    public Page create(JvmLogFile jvmLogFile, DecimalFormat decimalFormat) {
        return Page.builder()
                .menuName("Str. Dedup. exec stats")
                .fullName("String Deduplication execution stats")
                .info("Following charts show how much memory your application saves due to String Deduplication on each run")
                .icon(Page.Icon.CHART)
                .pageContents(
                        List.of(
                                Chart.builder()
                                        .chartType(Chart.ChartType.LINE)
                                        .title("Size of deduplicated strings (in Kb) in one execution")
                                        .data(getSizeChart(jvmLogFile.getStringDedupLogFile().getEntries()))
                                        .build(),
                                Chart.builder()
                                        .chartType(Chart.ChartType.LINE)
                                        .title("Count of deduplicated strings in one execution")
                                        .data(getCountChart(jvmLogFile.getStringDedupLogFile().getEntries()))
                                        .build(),
                                Chart.builder()
                                        .chartType(Chart.ChartType.LINE)
                                        .title("Size (in Kb) of scanned strings in one execution")
                                        .data(getNewSizeChart(jvmLogFile.getStringDedupLogFile().getEntries()))
                                        .build(),
                                Chart.builder()
                                        .chartType(Chart.ChartType.LINE)
                                        .title("Count of scanned strings in one execution")
                                        .data(getNewCountChart(jvmLogFile.getStringDedupLogFile().getEntries()))
                                        .build()
                        )
                )
                .build();
    }

    private static Object[][] getSizeChart(List<StringDedupLogEntry> entries) {
        Object[][] stats = new Object[entries.size() + 1][4];
        stats[0][0] = "Time";
        stats[0][1] = "Total size";
        stats[0][2] = "Young size";
        stats[0][3] = "Old size";
        int i = 1;
        for (StringDedupLogEntry status : entries) {
            stats[i][0] = status.getTimeStamp();
            stats[i][1] = status.getLastSize();
            stats[i][2] = status.getLastSizeYoung();
            stats[i][3] = status.getLastSizeOld();
            i++;
        }
        return stats;
    }

    private static Object[][] getCountChart(List<StringDedupLogEntry> entries) {
        Object[][] stats = new Object[entries.size() + 1][4];
        stats[0][0] = "Time";
        stats[0][1] = "Last count";
        stats[0][2] = "Young count";
        stats[0][3] = "Old count";
        int i = 1;
        for (StringDedupLogEntry status : entries) {
            stats[i][0] = status.getTimeStamp();
            stats[i][1] = status.getLastCount();
            stats[i][2] = status.getLastCountYoung();
            stats[i][3] = status.getLastCountOld();
            i++;
        }
        return stats;
    }

    private static Object[][] getNewCountChart(List<StringDedupLogEntry> entries) {
        Object[][] stats = new Object[entries.size() + 1][2];
        stats[0][0] = "Time";
        stats[0][1] = "New strings count";
        int i = 1;
        for (StringDedupLogEntry status : entries) {
            stats[i][0] = status.getTimeStamp();
            stats[i][1] = status.getLastCountNew();
            i++;
        }
        return stats;
    }

    private static Object[][] getNewSizeChart(List<StringDedupLogEntry> entries) {
        Object[][] stats = new Object[entries.size() + 1][2];
        stats[0][0] = "Time";
        stats[0][1] = "New strings size";
        int i = 1;
        for (StringDedupLogEntry status : entries) {
            stats[i][0] = status.getTimeStamp();
            stats[i][1] = status.getLastSizeNew();
            i++;
        }
        return stats;
    }
}
