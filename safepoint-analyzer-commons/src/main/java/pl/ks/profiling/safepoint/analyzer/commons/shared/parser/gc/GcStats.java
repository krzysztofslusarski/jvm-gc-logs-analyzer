package pl.ks.profiling.safepoint.analyzer.commons.shared.parser.gc;

import java.util.List;
import java.util.Set;
import lombok.Getter;
import lombok.Setter;
import pl.ks.profiling.safepoint.analyzer.commons.shared.OneFiledAllStats;

@Getter
@Setter
public class GcStats {
    private List<GcAgingStats> gcAgingStats;
    private GcAgingSummary gcAgingSummary;
    private List<GcRegionStats> gcRegionStats;
    private long maxSurvivorAge;
    private Set<String> gcRegions;
    private Set<String> gcPhases;
    private Set<String> gcAggregatedPhases;
    private List<GcPhaseStats> gcPhaseStats;
    private List<GcPhaseStats> gcAggregatedPhaseStats;
    private List<GcConcurrentCycleStats> gcConcurrentCycleStats;
    private Set<String> subPhases;
    private OneFiledAllStats liveHumongousStats;
    private OneFiledAllStats deadHumongousStats;
    private OneFiledAllStats allHumongousStats;
    private List<GcToSpaceStats> toSpaceStats;
    private List<Long> fullGcSequenceIds;
}
