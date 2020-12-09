package pl.ks.profiling.safepoint.analyzer.commons.shared.parser.gc;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
class GCAgingStats {
    private Map<Integer, BigDecimal> survivedRatio = new HashMap<>();
    private long sequenceId;
    private BigDecimal timeStamp;
}
