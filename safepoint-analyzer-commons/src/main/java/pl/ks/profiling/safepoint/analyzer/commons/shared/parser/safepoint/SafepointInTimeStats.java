package pl.ks.profiling.safepoint.analyzer.commons.shared.parser.safepoint;

import java.math.BigDecimal;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
class SafepointInTimeStats {
    private Long id;

    private BigDecimal time;
    private BigDecimal timeSpent;
    private long count;
}
