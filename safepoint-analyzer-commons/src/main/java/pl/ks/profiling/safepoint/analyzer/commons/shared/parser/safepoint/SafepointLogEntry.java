package pl.ks.profiling.safepoint.analyzer.commons.shared.parser.safepoint;

import java.math.BigDecimal;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Builder
@AllArgsConstructor
public class SafepointLogEntry {
    @Setter(AccessLevel.PACKAGE)
    private BigDecimal timeStamp;
    private long sequenceId;
    private String operationName;
    private BigDecimal applicationTime;
    private BigDecimal ttsTime;
    private BigDecimal stoppedTime;
    private boolean completed;

    SafepointLogEntry(BigDecimal timeStamp, long sequenceId) {
        this.timeStamp = timeStamp;
        this.sequenceId = sequenceId;
    }

    SafepointLogEntry(BigDecimal timeStamp, long sequenceId, BigDecimal applicationTime) {
        this.timeStamp = timeStamp;
        this.sequenceId = sequenceId;
        this.applicationTime = applicationTime;
    }

    void addOperationName(String operationName) {
        this.operationName = operationName;
    }

    void addTimeToSafepointAndStoppedTime(BigDecimal ttsTime, BigDecimal stoppedTime) {
        this.ttsTime = ttsTime;
        this.stoppedTime = stoppedTime;
        this.completed = true;
    }

    void addAllData(BigDecimal ttsTime, BigDecimal stoppedTime, BigDecimal applicationTime, String operationName) {
        this.applicationTime = applicationTime;
        this.operationName = operationName;
        this.ttsTime = ttsTime;
        this.stoppedTime = stoppedTime;
        this.completed = true;
    }
}
