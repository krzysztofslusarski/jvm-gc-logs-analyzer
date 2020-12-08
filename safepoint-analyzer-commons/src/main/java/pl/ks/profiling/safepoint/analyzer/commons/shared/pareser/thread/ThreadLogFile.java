package pl.ks.profiling.safepoint.analyzer.commons.shared.pareser.thread;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;

@Getter
public class ThreadLogFile {
    private List<ThreadsStatus> threadsStatuses = new ArrayList<>();
    private ThreadsStatus lastStatus = null;

    void newThreadAdded(BigDecimal timeStamp) {
        if (lastStatus == null) {
            lastStatus = ThreadsStatus.builder()
                    .createdCount(1)
                    .destroyedCount(0)
                    .timeStamp(timeStamp)
                    .build();
        } else {
            lastStatus = ThreadsStatus.builder()
                    .createdCount(lastStatus.getCreatedCount() + 1)
                    .destroyedCount(lastStatus.getDestroyedCount())
                    .timeStamp(timeStamp)
                    .build();
        }
        threadsStatuses.add(lastStatus);
    }

    void threadDestroyed(BigDecimal timeStamp) {
        if (lastStatus == null) {
            lastStatus = ThreadsStatus.builder()
                    .createdCount(0)
                    .destroyedCount(1)
                    .timeStamp(timeStamp)
                    .build();
        } else {
            lastStatus = ThreadsStatus.builder()
                    .createdCount(lastStatus.getCreatedCount())
                    .destroyedCount(lastStatus.getDestroyedCount() + 1)
                    .timeStamp(timeStamp)
                    .build();
        }
        threadsStatuses.add(lastStatus);
    }
}
