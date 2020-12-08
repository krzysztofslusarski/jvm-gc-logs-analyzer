package pl.ks.profiling.safepoint.analyzer.commons.shared.pareser.gc;

import java.util.HashMap;
import java.util.Map;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class GcToSpaceStats {
    private long sequenceId;
    private Map<String, String> regionStats = new HashMap<>();
}
