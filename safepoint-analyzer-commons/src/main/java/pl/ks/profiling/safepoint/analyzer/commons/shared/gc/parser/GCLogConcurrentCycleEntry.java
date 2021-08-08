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

import java.math.BigDecimal;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@AllArgsConstructor
public class GCLogConcurrentCycleEntry {
    private long sequenceId;
    private BigDecimal time;
    private int mixedCollectionsAfterConcurrent;
    @Setter(AccessLevel.PACKAGE)
    private int remarkReclaimed;

    public String getPhase() {
        return "Concurrent Mark";
    }

    void nextMixedCollection() {
        mixedCollectionsAfterConcurrent++;
    }

    public boolean isWasted() {
        return getMixedCollectionsAfterConcurrent() == 0 && getRemarkReclaimed() == 0;
    }
}
