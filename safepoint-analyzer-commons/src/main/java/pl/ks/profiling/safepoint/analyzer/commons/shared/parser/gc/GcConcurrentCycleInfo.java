package pl.ks.profiling.safepoint.analyzer.commons.shared.parser.gc;

import java.math.BigDecimal;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class GcConcurrentCycleInfo {
    private long sequenceId;
    private String phase;
    private BigDecimal time;
}
