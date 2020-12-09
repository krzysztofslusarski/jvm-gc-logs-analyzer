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
package pl.ks.profiling.safepoint.analyzer.commons.shared.jit.parser;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.Getter;

@Getter
public class JitLogFile {
    private List<CompilationStatus> compilationStatuses = new ArrayList<>();
    private List<CodeCacheSweeperActivity> codeCacheSweeperActivities = new ArrayList<>();
    private Map<String, List<CodeCacheStatus>> codeCacheStatuses = new HashMap<>();
    private CompilationStatus lastStatus = null;

    void newCodeCacheSweeperActivity(BigDecimal timeStamp) {
        codeCacheSweeperActivities.add(CodeCacheSweeperActivity.builder()
                .timeStamp(timeStamp)
                .build());
    }

    void newCodeCacheStats(String segment, BigDecimal timeStamp, long size, long maxUsed, long used) {
        codeCacheStatuses.computeIfAbsent(segment, name -> new ArrayList<>()).add(
                CodeCacheStatus.builder()
                        .timeStamp(timeStamp)
                        .size(size)
                        .maxUsed(maxUsed)
                        .used(used)
                        .build()
        );
    }

    void newCompilation(BigDecimal timeStamp, int tier) {
        if (lastStatus == null) {
            lastStatus = CompilationStatus.builder()
                    .loadedCount(1)
                    .timeStamp(timeStamp)
                    .build();

            switch (tier) {
                case 1:
                    lastStatus = lastStatus.withTier1LoadedCount(1);
                    break;
                case 2:
                    lastStatus = lastStatus.withTier2LoadedCount(1);
                    break;
                case 3:
                    lastStatus = lastStatus.withTier3LoadedCount(1);
                    break;
                case 4:
                    lastStatus = lastStatus.withTier4LoadedCount(1);
                    break;
            }
        } else {
            lastStatus = lastStatus.withTimeStamp(timeStamp);
            switch (tier) {
                case 1:
                    lastStatus = lastStatus.withTier1LoadedCount(lastStatus.getTier1LoadedCount() + 1).withLoadedCount(lastStatus.getLoadedCount() + 1);
                    break;
                case 2:
                    lastStatus = lastStatus.withTier2LoadedCount(lastStatus.getTier2LoadedCount() + 1).withLoadedCount(lastStatus.getLoadedCount() + 1);
                    break;
                case 3:
                    lastStatus = lastStatus.withTier3LoadedCount(lastStatus.getTier3LoadedCount() + 1).withLoadedCount(lastStatus.getLoadedCount() + 1);
                    break;
                case 4:
                    lastStatus = lastStatus.withTier4LoadedCount(lastStatus.getTier4LoadedCount() + 1).withLoadedCount(lastStatus.getLoadedCount() + 1);
                    break;
            }
        }

        compilationStatuses.add(lastStatus);
    }

    void compilationMadeNodEntrant(BigDecimal timeStamp, int tier) {
        if (lastStatus == null) {
            lastStatus = CompilationStatus.builder()
                    .unloadedCount(1)
                    .timeStamp(timeStamp)
                    .build();

            switch (tier) {
                case 1:
                    lastStatus = lastStatus.withTier1UnloadedCount(1);
                    break;
                case 2:
                    lastStatus = lastStatus.withTier2UnloadedCount(1);
                    break;
                case 3:
                    lastStatus = lastStatus.withTier3UnloadedCount(1);
                    break;
                case 4:
                    lastStatus = lastStatus.withTier4UnloadedCount(1);
                    break;
            }
        } else {
            lastStatus = lastStatus.withTimeStamp(timeStamp);
            switch (tier) {
                case 1:
                    lastStatus = lastStatus.withTier1UnloadedCount(lastStatus.getTier1UnloadedCount() + 1).withUnloadedCount(lastStatus.getUnloadedCount() + 1);
                    break;
                case 2:
                    lastStatus = lastStatus.withTier2UnloadedCount(lastStatus.getTier2UnloadedCount() + 1).withUnloadedCount(lastStatus.getUnloadedCount() + 1);
                    break;
                case 3:
                    lastStatus = lastStatus.withTier3UnloadedCount(lastStatus.getTier3UnloadedCount() + 1).withUnloadedCount(lastStatus.getUnloadedCount() + 1);
                    break;
                case 4:
                    lastStatus = lastStatus.withTier4UnloadedCount(lastStatus.getTier4UnloadedCount() + 1).withUnloadedCount(lastStatus.getUnloadedCount() + 1);
                    break;
            }
        }

        compilationStatuses.add(lastStatus);
    }

}
