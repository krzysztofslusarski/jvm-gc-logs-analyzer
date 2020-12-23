package pl.ks.profiling.safepoint.analyzer.standalone.concatenation;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public class ConcatenationProgress {
    private final int currentFileNumber;
    private final int numberOfFiles;
    private final long allFilesSizeBytes;
    private final long processedBytes;
}
