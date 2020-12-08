package pl.ks.profiling.safepoint.analyzer.commons.shared.pareser.gc;

import java.util.List;
import java.util.Map;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class GcLogFile {
    private List<GcCycleInfo> gcCycleInfos;
    private List<GcConcurrentCycleInfo> gcConcurrentCycleInfos;
    private Map<Long, List<String>> lines;
}
