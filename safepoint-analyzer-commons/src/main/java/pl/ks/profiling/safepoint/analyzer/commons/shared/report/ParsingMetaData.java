package pl.ks.profiling.safepoint.analyzer.commons.shared.report;

import lombok.Data;

import java.util.List;

@Data
public class ParsingMetaData {
    private final String name;
    private final List<LogsFile> files;
    private final long numberOfLines;
}
