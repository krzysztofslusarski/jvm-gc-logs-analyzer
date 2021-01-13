package pl.ks.profiling.io.source;

import lombok.Data;

import java.util.List;

@Data
public class LogSourceFile {
    public static final List<LogSourceSubfile> NO_SUBFILES = List.of();

    private final String name;
    private final List<LogSourceSubfile> subfiles;
}
