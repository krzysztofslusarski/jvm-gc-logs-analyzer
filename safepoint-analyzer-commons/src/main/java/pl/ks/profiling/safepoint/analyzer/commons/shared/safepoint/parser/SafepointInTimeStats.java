package pl.ks.profiling.safepoint.analyzer.commons.shared.safepoint.parser;

import java.math.BigDecimal;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter(AccessLevel.PACKAGE)
class SafepointInTimeStats {
    private BigDecimal time;
    private BigDecimal timeSpent;
    private long count;
}
