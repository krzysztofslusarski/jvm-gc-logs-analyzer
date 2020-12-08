package pl.ks.profiling.safepoint.analyzer.commons.shared.pareser.gc;

import lombok.Getter;
import lombok.Setter;
import pl.ks.profiling.safepoint.analyzer.commons.shared.OneFiledAllStats;

@Getter
@Setter
public class GcConcurrentCycleStats {
    private String name;
    private Long count;
    private OneFiledAllStats time;
}
