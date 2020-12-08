package pl.ks.profiling.safepoint.analyzer.commons.shared.pareser.thread;

import java.math.BigDecimal;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class ThreadsStatus {
    BigDecimal timeStamp;
    long createdCount;
    long destroyedCount;

    public long getCurrentCount() {
        return createdCount - destroyedCount;
    }
}
