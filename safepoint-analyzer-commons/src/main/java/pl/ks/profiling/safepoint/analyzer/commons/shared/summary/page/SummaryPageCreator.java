package pl.ks.profiling.safepoint.analyzer.commons.shared.summary.page;

import pl.ks.profiling.gui.commons.Page;
import pl.ks.profiling.gui.commons.Table;
import pl.ks.profiling.safepoint.analyzer.commons.shared.PageCreator;
import pl.ks.profiling.safepoint.analyzer.commons.shared.report.JvmLogFile;
import pl.ks.profiling.safepoint.analyzer.commons.shared.report.LogsFile;

import java.text.DecimalFormat;
import java.util.List;
import java.util.stream.Collectors;

public class SummaryPageCreator implements PageCreator {
    @Override
    public Page create(JvmLogFile jvmLogFile, DecimalFormat decimalFormat) {
        return Page.builder()
                .menuName("Summary")
                .fullName("Summary")
                .icon(Page.Icon.STATS)
                .pageContents(List.of(
                        summaryTable(jvmLogFile),
                        filesTable(jvmLogFile)
                ))
                .build();
    }

    private Table summaryTable(JvmLogFile jvmLogFile) {
        return Table.builder()
                .title("Summary")
                .header(List.of("", ""))
                .table(List.of(
                        List.of("Parsing name", jvmLogFile.getParsing().getName()),
                        List.of("Number of lines", String.valueOf(jvmLogFile.getParsing().getNumberOfLines()))
                ))
                .build();
    }

    private Table filesTable(JvmLogFile jvmLogFile) {
        List<LogsFile> files = jvmLogFile.getParsing().getFiles();

        List<List<String>> rows = files.stream().map(f -> List.of(f.getName(), String.join(", ", f.getSubfiles()))).collect(Collectors.toList());
        return Table.builder()
                .title("Files")
                .header(List.of("File", "Subfiles"))
                .table(rows)
                .build();
    }
}
