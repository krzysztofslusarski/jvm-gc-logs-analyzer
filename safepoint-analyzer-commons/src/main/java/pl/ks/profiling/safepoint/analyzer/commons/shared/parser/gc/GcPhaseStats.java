package pl.ks.profiling.safepoint.analyzer.commons.shared.parser.gc;

import java.util.Map;
import lombok.Getter;
import lombok.Setter;
import pl.ks.profiling.safepoint.analyzer.commons.shared.OneFiledAllStats;

@Getter
@Setter
public class GcPhaseStats {
    private String name;
    private Long count;
    private OneFiledAllStats time;
    private Map<String, OneFiledAllStats> subPhaseTimes;
}
