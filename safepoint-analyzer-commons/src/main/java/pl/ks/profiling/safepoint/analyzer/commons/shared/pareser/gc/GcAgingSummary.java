package pl.ks.profiling.safepoint.analyzer.commons.shared.pareser.gc;

import java.util.HashMap;
import java.util.Map;
import lombok.Getter;
import lombok.Setter;
import pl.ks.profiling.safepoint.analyzer.commons.shared.OneFiledAllStats;

@Getter
@Setter
public class GcAgingSummary {
    private Map<Integer, Double> survivedRatio = new HashMap<>();
    private Map<Integer, OneFiledAllStats> agingSizes = new HashMap<>();
}
