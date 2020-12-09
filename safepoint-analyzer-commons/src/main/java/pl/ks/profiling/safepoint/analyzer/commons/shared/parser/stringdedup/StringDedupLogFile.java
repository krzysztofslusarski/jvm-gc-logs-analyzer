package pl.ks.profiling.safepoint.analyzer.commons.shared.parser.stringdedup;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;

public class StringDedupLogFile {
    @Getter
    private final List<StringDedupLogEntry> entries = new ArrayList<>();
    private StringDedupLogEntry lastEntry = new StringDedupLogEntry();

    private Stage stage = Stage.UNKNOWN;

    void newEntry(BigDecimal timestamp) {
        if (lastEntry.isInitialized()) {
            entries.add(lastEntry);
            lastEntry = new StringDedupLogEntry();
        }
        stage = Stage.UNKNOWN;
        lastEntry.setTimeStamp(timestamp);
    }

    void lastExecOccurred() {
        stage = Stage.LAST_EXEC;
    }

    void totalExecOccurred() {
        stage = Stage.TOTAL_EXEC;
    }

    void deduplicated(long count, BigDecimal size) {
        switch (stage) {
            case LAST_EXEC:
                lastEntry.deduplicatedLast(count, size);
                break;
            case TOTAL_EXEC:
                lastEntry.deduplicatedTotal(count, size);
                break;
        }
    }

    void young(long count, BigDecimal size) {
        switch (stage) {
            case LAST_EXEC:
                lastEntry.youngLast(count, size);
                break;
            case TOTAL_EXEC:
                lastEntry.youngTotal(count, size);
                break;
        }
    }

    void old(long count, BigDecimal size) {
        switch (stage) {
            case LAST_EXEC:
                lastEntry.oldLast(count, size);
                break;
            case TOTAL_EXEC:
                lastEntry.oldTotal(count, size);
                break;
        }
    }

    public void newStrings(long count, BigDecimal size) {
        switch (stage) {
            case LAST_EXEC:
                lastEntry.newLast(count, size);
                break;
            case TOTAL_EXEC:
                lastEntry.newTotal(count, size);
                break;
        }
    }
}
