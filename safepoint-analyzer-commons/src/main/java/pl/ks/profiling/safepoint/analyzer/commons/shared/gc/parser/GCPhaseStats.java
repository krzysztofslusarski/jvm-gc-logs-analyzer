package pl.ks.profiling.safepoint.analyzer.commons.shared.gc.parser;

import java.util.Map;
import lombok.Getter;
import lombok.Setter;
import pl.ks.profiling.safepoint.analyzer.commons.shared.OneFiledAllStats;

@Getter
@Setter
public class GCPhaseStats {
    private String name;
    private Long count;
    private OneFiledAllStats time;
    private Map<String, OneFiledAllStats> subPhaseTimes;
}
