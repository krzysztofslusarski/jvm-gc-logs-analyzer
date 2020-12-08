package pl.ks.profiling.safepoint.analyzer.commons.shared.pareser.gc;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class GcCycleInfo {
    private Map<Integer, Long> bytesInAges = new HashMap<>();
    private int maxAge;
    private long sequenceId;
    private BigDecimal timeStamp;
    private String phase;
    private String aggregatedPhase;
    private Map<String, Integer> regionsAfterGC = new HashMap<>();
    private Map<String, Integer> regionsSizeAfterGC = new HashMap<>();
    private Map<String, Integer> regionsWastedAfterGC = new HashMap<>();
    private Map<String, Integer> regionsBeforeGC = new HashMap<>();
    private Map<String, Integer> regionsMax = new HashMap<>();
    private List<Long> liveHumongousSizes = new ArrayList<>();
    private List<Long> deadHumongousSizes = new ArrayList<>();
    private int heapBeforeGC;
    private int heapAfterGC;
    private int heapSize;
    private BigDecimal time;
    private Map<String, BigDecimal> subPhasesTime = new LinkedHashMap<>();
    private boolean genuineCollection;
    private boolean wasToSpaceExhausted;
}
