package pl.ks.profiling.safepoint.analyzer.commons.shared.parser.safepoint;

import java.math.BigDecimal;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SafepointOperation {
    private Long id;
    private long sequenceId;
    private String operationName;
    private BigDecimal applicationTime;
    private BigDecimal ttsTime;
    private BigDecimal stoppedTime;
    private BigDecimal timeStamp;
}
