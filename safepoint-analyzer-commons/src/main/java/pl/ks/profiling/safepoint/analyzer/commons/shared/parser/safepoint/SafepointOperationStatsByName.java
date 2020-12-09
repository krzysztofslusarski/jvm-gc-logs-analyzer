package pl.ks.profiling.safepoint.analyzer.commons.shared.parser.safepoint;

import java.math.BigDecimal;
import java.util.Set;
import lombok.Getter;
import lombok.Setter;
import pl.ks.profiling.safepoint.analyzer.commons.shared.OneFiledAllStats;

@Getter
@Setter
public class SafepointOperationStatsByName {
    private Long id;

    private String operationName;
    private long count;
    private BigDecimal countPercent;
    private BigDecimal timeWithTtsPercent;
    private OneFiledAllStats operationTime;
    private Set<SafepointInTimeStats> statsByTime;
}
