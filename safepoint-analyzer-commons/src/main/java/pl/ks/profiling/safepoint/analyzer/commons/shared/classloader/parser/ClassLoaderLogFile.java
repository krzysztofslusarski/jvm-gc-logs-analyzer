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
package pl.ks.profiling.safepoint.analyzer.commons.shared.classloader.parser;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;

@Getter
public class ClassLoaderLogFile {
    private List<ClassStatus> classStatuses = new ArrayList<>();
    private ClassStatus lastStatus = null;

    void newClassLoaded(BigDecimal timeStamp) {
        if (lastStatus == null) {
            lastStatus = ClassStatus.builder()
                    .loadedCount(1)
                    .unloadedCount(0)
                    .timeStamp(timeStamp)
                    .build();
        } else {
            lastStatus = ClassStatus.builder()
                    .loadedCount(lastStatus.getLoadedCount() + 1)
                    .unloadedCount(lastStatus.getUnloadedCount())
                    .timeStamp(timeStamp)
                    .build();
        }
        classStatuses.add(lastStatus);
    }

    void classUnloaded(BigDecimal timeStamp) {
        if (lastStatus == null) {
            lastStatus = ClassStatus.builder()
                    .loadedCount(0)
                    .unloadedCount(1)
                    .timeStamp(timeStamp)
                    .build();
        } else {
            lastStatus = ClassStatus.builder()
                    .loadedCount(lastStatus.getLoadedCount())
                    .unloadedCount(lastStatus.getUnloadedCount() + 1)
                    .timeStamp(timeStamp)
                    .build();
        }
        classStatuses.add(lastStatus);
    }
}
