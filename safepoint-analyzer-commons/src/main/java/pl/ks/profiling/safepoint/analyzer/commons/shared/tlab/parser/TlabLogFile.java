/*
 * Copyright 2020 Krzysztof Slusarski
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package pl.ks.profiling.safepoint.analyzer.commons.shared.tlab.parser;

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
                                    .mapToDouble(threadTlabBeforeGC -> (double) threadTlabBeforeGC.getSizeKb())
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
