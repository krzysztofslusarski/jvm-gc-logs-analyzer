package pl.ks.profiling.safepoint.analyzer.commons.shared.pareser.tlab;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.Getter;
import pl.ks.profiling.safepoint.analyzer.commons.shared.OneFiledAllStatsUtil;

@Getter
public class TlabLogFile {
    private final List<TlabSummaryInfo> tlabSummaries = new ArrayList<>();
    private final List<List<ThreadTlabBeforeGC>> threadTlabsBeforeGC = new ArrayList<>();

    public Collection<ThreadTlabInfo> getThreadTlabStats() {
        Map<String, List<ThreadTlabBeforeGC>> infoPerThread = new HashMap<>();
        Map<String, ThreadTlabInfo> threadTlabInfos = new HashMap<>();
        threadTlabsBeforeGC.stream().flatMap(Collection::stream).forEach(threadTlabBeforeGC -> {
            infoPerThread.computeIfAbsent(threadTlabBeforeGC.getTid(), ignored -> new ArrayList<>()).add(threadTlabBeforeGC);
        });
        infoPerThread.forEach((tid, threadTlabsBeforeGC) -> {
            threadTlabInfos.put(tid, ThreadTlabInfo.builder()
                    .tid(tid)
                    .nid(threadTlabsBeforeGC.get(0).getNid())
                    .size(OneFiledAllStatsUtil.create(
                            threadTlabsBeforeGC.stream()
                                    .mapToDouble(threadTlabBeforeGC -> (double) threadTlabBeforeGC.getSize())
                                    .toArray())
                    )
                    .slowAllocs(OneFiledAllStatsUtil.create(
                            threadTlabsBeforeGC.stream()
                                    .mapToDouble(threadTlabBeforeGC -> (double) threadTlabBeforeGC.getSlowAllocs())
                                    .toArray())
                    )
                    .build());
        });
        return threadTlabInfos.values();
    }

    public void newThreadTlabBeforeGc(List<ThreadTlabBeforeGC> threadTlabBeforeGC) {
        threadTlabsBeforeGC.add(threadTlabBeforeGC);
    }

    public void newSummary(BigDecimal timeStamp, long threadCount, long refills, long maxRefills, long slowAllocs, long maxSlowAllocs, BigDecimal wastePercent) {
        tlabSummaries.add(TlabSummaryInfo.builder()
                .timeStamp(timeStamp)
                .threadCount(threadCount)
                .refills(refills)
                .maxRefills(maxRefills)
                .slowAllocs(slowAllocs)
                .maxSlowAllocs(maxSlowAllocs)
                .wastePercent(wastePercent)
                .build());
    }
}
