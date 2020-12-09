package pl.ks.profiling.safepoint.analyzer.commons.shared.parser.gc;

import java.util.List;
import java.util.Set;
import lombok.Getter;
import lombok.Setter;
import pl.ks.profiling.safepoint.analyzer.commons.shared.OneFiledAllStats;

@Getter
@Setter
public class GCStats {
    private List<GCAgingStats> gcAgingStats;
    private GCAgingSummary gcAgingSummary;
    private List<GCRegionStats> gcRegionStats;
    private long maxSurvivorAge;
    private Set<String> gcRegions;
    private Set<String> gcPhases;
    private Set<String> gcAggregatedPhases;
    private List<GCPhaseStats> gcPhaseStats;
    private List<GCPhaseStats> gcAggregatedPhaseStats;
    private List<GCConcurrentCycleStats> gcConcurrentCycleStats;
    private Set<String> subPhases;
    private OneFiledAllStats liveHumongousStats;
    private OneFiledAllStats deadHumongousStats;
    private OneFiledAllStats allHumongousStats;
    private List<GCToSpaceStats> toSpaceStats;
    private List<Long> fullGcSequenceIds;
}
