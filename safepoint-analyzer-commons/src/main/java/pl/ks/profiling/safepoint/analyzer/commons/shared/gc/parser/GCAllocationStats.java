package pl.ks.profiling.safepoint.analyzer.commons.shared.gc.parser;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter(AccessLevel.PACKAGE)
public class GCAllocationStats {
    private BigDecimal totalAllocation;
}
