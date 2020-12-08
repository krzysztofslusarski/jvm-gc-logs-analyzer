package pl.ks.profiling.safepoint.analyzer.commons.shared.pareser.gc;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
class GcAgingStats {
    private Map<Integer, BigDecimal> survivedRatio = new HashMap<>();
    private long sequenceId;
    private BigDecimal timeStamp;
}
