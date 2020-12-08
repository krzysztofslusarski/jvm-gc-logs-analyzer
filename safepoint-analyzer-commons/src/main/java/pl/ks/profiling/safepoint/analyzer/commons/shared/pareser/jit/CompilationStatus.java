package pl.ks.profiling.safepoint.analyzer.commons.shared.pareser.jit;

import java.math.BigDecimal;
import lombok.Builder;
import lombok.Value;
import lombok.With;

@With
@Value
@Builder
public class CompilationStatus {
    BigDecimal timeStamp;
    long loadedCount;
    long unloadedCount;
    long tier1LoadedCount;
    long tier1UnloadedCount;
    long tier2LoadedCount;
    long tier2UnloadedCount;
    long tier3LoadedCount;
    long tier3UnloadedCount;
    long tier4LoadedCount;
    long tier4UnloadedCount;

    public long getCurrentCount() {
        return loadedCount - unloadedCount;
    }

    public long getTier1CurrentCount() {
        return tier1LoadedCount - tier1UnloadedCount;
    }

    public long getTier2CurrentCount() {
        return tier2LoadedCount - tier2UnloadedCount;
    }

    public long getTier3CurrentCount() {
        return tier3LoadedCount - tier3UnloadedCount;
    }

    public long getTier4CurrentCount() {
        return tier4LoadedCount - tier4UnloadedCount;
    }
}
