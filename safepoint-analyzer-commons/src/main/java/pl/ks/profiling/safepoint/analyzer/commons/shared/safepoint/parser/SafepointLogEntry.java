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
package pl.ks.profiling.safepoint.analyzer.commons.shared.safepoint.parser;

import java.math.BigDecimal;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Builder
@AllArgsConstructor
public class SafepointLogEntry {
    @Setter(AccessLevel.PACKAGE)
    private BigDecimal timeStamp;
    private long sequenceId;
    private String operationName;
    private BigDecimal applicationTime;
    private BigDecimal ttsTime;
    private BigDecimal stoppedTime;
    private boolean completed;

    SafepointLogEntry(BigDecimal timeStamp, long sequenceId) {
        this.timeStamp = timeStamp;
        this.sequenceId = sequenceId;
    }

    SafepointLogEntry(BigDecimal timeStamp, long sequenceId, BigDecimal applicationTime) {
        this.timeStamp = timeStamp;
        this.sequenceId = sequenceId;
        this.applicationTime = applicationTime;
    }

    void addOperationName(String operationName) {
        this.operationName = operationName;
    }

    void addTimeToSafepointAndStoppedTime(BigDecimal ttsTime, BigDecimal stoppedTime) {
        this.ttsTime = ttsTime;
        this.stoppedTime = stoppedTime;
        this.completed = true;
    }

    void addAllData(BigDecimal ttsTime, BigDecimal stoppedTime, BigDecimal applicationTime, String operationName) {
        this.applicationTime = applicationTime;
        this.operationName = operationName;
        this.ttsTime = ttsTime;
        this.stoppedTime = stoppedTime;
        this.completed = true;
    }
}
