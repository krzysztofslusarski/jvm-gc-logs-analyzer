package pl.ks.profiling.safepoint.analyzer.commons.shared.gc.parser;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter(AccessLevel.PACKAGE)
class GCAgingStats {
    private Map<Integer, BigDecimal> survivedRatio = new HashMap<>();
    private long sequenceId;
    private BigDecimal timeStamp;
}
