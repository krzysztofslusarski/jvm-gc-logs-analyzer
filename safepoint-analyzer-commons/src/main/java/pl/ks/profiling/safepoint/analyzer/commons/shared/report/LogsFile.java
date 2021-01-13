package pl.ks.profiling.safepoint.analyzer.commons.shared.report;

import lombok.Data;

import java.util.List;

@Data
public class LogsFile {
    private final String name;
    private final List<String> subfiles;
}
