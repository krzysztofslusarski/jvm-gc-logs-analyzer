package pl.ks.profiling.safepoint.analyzer.commons.shared.parser.safepoint;

import java.math.BigDecimal;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TimesInTime {
    private BigDecimal startTime;
    private BigDecimal endTime;
    private BigDecimal tts = BigDecimal.ZERO;
    private BigDecimal operationTime = BigDecimal.ZERO;
    private BigDecimal applicationTime = BigDecimal.ZERO;
}
