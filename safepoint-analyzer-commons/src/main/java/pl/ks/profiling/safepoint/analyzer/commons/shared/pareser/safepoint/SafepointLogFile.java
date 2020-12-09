package pl.ks.profiling.safepoint.analyzer.commons.shared.pareser.safepoint;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;

public class SafepointLogFile {
    @Getter
    private List<SafepointLogEntry> safepoints = new ArrayList<>();
    @Getter
    private SafepointOperationStats safepointOperationStats;
    private SafepointLogEntry lastEntry;
    private long sequenceId;

    void newSafepoint(BigDecimal timeStamp) {
        if (lastEntry != null && lastEntry.isCompleted()) {
            safepoints.add(lastEntry);
        }
        lastEntry = new SafepointLogEntry(timeStamp, sequenceId++);
    }

    void newSafepoint(BigDecimal timeStamp, BigDecimal applicationTime) {
        if (lastEntry != null && lastEntry.isCompleted()) {
            safepoints.add(lastEntry);
        }
        lastEntry = new SafepointLogEntry(timeStamp, sequenceId++, applicationTime);
    }

    void addOperationName(String operationName) {
        if (lastEntry == null) {
            return;
        }
        lastEntry.addOperationName(operationName);
    }

    void addTimeToSafepointAndStoppedTime(BigDecimal timeToSafepoint, BigDecimal stoppedTime) {
        if (lastEntry == null) {
            return;
        }
        lastEntry.addTimeToSafepointAndStoppedTime(timeToSafepoint, stoppedTime);
    }

    void addAllData(BigDecimal timeToSafepoint, BigDecimal stoppedTime, BigDecimal applicationTime, String operationName) {
        if (lastEntry == null) {
            return;
        }
        lastEntry.addAllData(timeToSafepoint, stoppedTime, applicationTime, operationName);
    }

    void parsingCompleted() {
        if (lastEntry != null && lastEntry.isCompleted()) {
            safepoints.add(lastEntry);
            lastEntry = null;
        }

        if (safepointOperationStats != null) {
            return;
        }

        safepointOperationStats = SafepointStatsCreator.create(this);
    }
}
