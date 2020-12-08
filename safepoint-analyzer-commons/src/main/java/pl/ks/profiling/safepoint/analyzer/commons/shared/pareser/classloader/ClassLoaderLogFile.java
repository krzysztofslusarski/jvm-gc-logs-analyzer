package pl.ks.profiling.safepoint.analyzer.commons.shared.pareser.classloader;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;

@Getter
public class ClassLoaderLogFile {
    private List<ClassStatus> classStatuses = new ArrayList<>();
    private ClassStatus lastStatus = null;

    void newClassLoaded(BigDecimal timeStamp) {
        if (lastStatus == null) {
            lastStatus = ClassStatus.builder()
                    .loadedCount(1)
                    .unloadedCount(0)
                    .timeStamp(timeStamp)
                    .build();
        } else {
            lastStatus = ClassStatus.builder()
                    .loadedCount(lastStatus.getLoadedCount() + 1)
                    .unloadedCount(lastStatus.getUnloadedCount())
                    .timeStamp(timeStamp)
                    .build();
        }
        classStatuses.add(lastStatus);
    }

    void classUnloaded(BigDecimal timeStamp) {
        if (lastStatus == null) {
            lastStatus = ClassStatus.builder()
                    .loadedCount(0)
                    .unloadedCount(1)
                    .timeStamp(timeStamp)
                    .build();
        } else {
            lastStatus = ClassStatus.builder()
                    .loadedCount(lastStatus.getLoadedCount())
                    .unloadedCount(lastStatus.getUnloadedCount() + 1)
                    .timeStamp(timeStamp)
                    .build();
        }
        classStatuses.add(lastStatus);
    }
}
