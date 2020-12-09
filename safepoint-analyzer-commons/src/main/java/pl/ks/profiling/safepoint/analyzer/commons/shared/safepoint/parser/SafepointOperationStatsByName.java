package pl.ks.profiling.safepoint.analyzer.commons.shared.safepoint.parser;

import java.math.BigDecimal;
import java.util.Set;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import pl.ks.profiling.safepoint.analyzer.commons.shared.OneFiledAllStats;

@Getter
@Setter(AccessLevel.PACKAGE)
public class SafepointOperationStatsByName {
    private String operationName;
    private long count;
    private BigDecimal countPercent;
    private BigDecimal timeWithTtsPercent;
    private OneFiledAllStats operationTime;
    private Set<SafepointInTimeStats> statsByTime;
}
