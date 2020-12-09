package pl.ks.profiling.safepoint.analyzer.commons.shared;

import java.math.BigDecimal;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class OneFiledAllStats {
    private BigDecimal percentile50;
    private BigDecimal percentile75;
    private BigDecimal percentile90;
    private BigDecimal percentile95;
    private BigDecimal percentile99;
    private BigDecimal percentile99and9;
    private BigDecimal percentile100;
    private BigDecimal average;
    private BigDecimal total;
    private BigDecimal count;
}
