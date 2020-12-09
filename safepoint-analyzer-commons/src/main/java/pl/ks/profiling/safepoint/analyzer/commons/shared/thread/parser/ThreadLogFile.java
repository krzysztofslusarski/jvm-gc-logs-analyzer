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
package pl.ks.profiling.safepoint.analyzer.commons.shared.thread.parser;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;

@Getter
public class ThreadLogFile {
    private List<ThreadsStatus> threadsStatuses = new ArrayList<>();
    private ThreadsStatus lastStatus = null;

    void newThreadAdded(BigDecimal timeStamp) {
        if (lastStatus == null) {
            lastStatus = ThreadsStatus.builder()
                    .createdCount(1)
                    .destroyedCount(0)
                    .timeStamp(timeStamp)
                    .build();
        } else {
            lastStatus = ThreadsStatus.builder()
                    .createdCount(lastStatus.getCreatedCount() + 1)
                    .destroyedCount(lastStatus.getDestroyedCount())
                    .timeStamp(timeStamp)
                    .build();
        }
        threadsStatuses.add(lastStatus);
    }

    void threadDestroyed(BigDecimal timeStamp) {
        if (lastStatus == null) {
            lastStatus = ThreadsStatus.builder()
                    .createdCount(0)
                    .destroyedCount(1)
                    .timeStamp(timeStamp)
                    .build();
        } else {
            lastStatus = ThreadsStatus.builder()
                    .createdCount(lastStatus.getCreatedCount())
                    .destroyedCount(lastStatus.getDestroyedCount() + 1)
                    .timeStamp(timeStamp)
                    .build();
        }
        threadsStatuses.add(lastStatus);
    }
}
