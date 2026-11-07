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
package pl.ks.profiling.safepoint.analyzer.commons.shared.gc.parser;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * G1 concurrent cycle. A mark cycle ("Concurrent Cycle" in JDK 9-16, "Concurrent Mark Cycle" since JDK 17)
 * is followed by mixed collections; an undo cycle (JDK 17+) is abandoned before marking starts.
 */
@Getter
public class GCLogConcurrentCycleEntry {
    public static final String CONCURRENT_MARK = "Concurrent Mark";
    public static final String CONCURRENT_UNDO = "Concurrent Undo";

    private final long sequenceId;
    private final String phase;
    /**
     * Duration in ms, {@code null} until the end of the cycle is logged.
     */
    @Setter(AccessLevel.PACKAGE)
    private BigDecimal time;
    private int mixedCollectionsAfterConcurrent;
    @Setter(AccessLevel.PACKAGE)
    private int remarkReclaimed;

    GCLogConcurrentCycleEntry(long sequenceId, String phase) {
        this.sequenceId = sequenceId;
        this.phase = phase;
    }

    void nextMixedCollection() {
        mixedCollectionsAfterConcurrent++;
    }

    public boolean isMark() {
        return CONCURRENT_MARK.equals(phase);
    }

    /**
     * A mark cycle is wasted when neither the remark nor the following mixed collections reclaimed anything.
     * Undo cycles do not mark at all, so they are never wasted.
     */
    public boolean isWasted() {
        return isMark() && getMixedCollectionsAfterConcurrent() == 0 && getRemarkReclaimed() == 0;
    }
}
