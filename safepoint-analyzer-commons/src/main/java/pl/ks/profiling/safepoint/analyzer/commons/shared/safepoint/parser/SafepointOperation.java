package pl.ks.profiling.safepoint.analyzer.commons.shared.safepoint.parser;

import java.math.BigDecimal;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter(AccessLevel.PACKAGE)
public class SafepointOperation {
    private long sequenceId;
    private String operationName;
    private BigDecimal applicationTime;
    private BigDecimal ttsTime;
    private BigDecimal stoppedTime;
    private BigDecimal timeStamp;
}
