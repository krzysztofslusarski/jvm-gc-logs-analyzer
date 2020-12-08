package pl.ks.profiling.safepoint.analyzer.commons.shared.pareser.jit;

import java.math.BigDecimal;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class CodeCacheStatus {
    BigDecimal timeStamp;
    long size;
    long used;
    long maxUsed;
}
