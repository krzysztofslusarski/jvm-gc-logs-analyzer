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
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;

public class SafepointLogFile {
    @Getter
    private List<SafepointLogEntry> safepoints = new ArrayList<>();
    @Getter
    private SafepointOperationStats safepointOperationStats;
    private SafepointLogEntry lastEntry;
    private long sequenceId;

    void newSafepoint(BigDecimal timeStamp) {
        if (lastEntry != null && lastEntry.isCompleted()) {
            safepoints.add(lastEntry);
        }
        lastEntry = new SafepointLogEntry(timeStamp, sequenceId++);
    }

    void newSafepoint(BigDecimal timeStamp, BigDecimal applicationTime) {
        if (lastEntry != null && lastEntry.isCompleted()) {
            safepoints.add(lastEntry);
        }
        lastEntry = new SafepointLogEntry(timeStamp, sequenceId++, applicationTime);
    }

    void addOperationName(String operationName) {
        if (lastEntry == null) {
            return;
        }
        lastEntry.addOperationName(operationName);
    }

    void addTimeToSafepointAndStoppedTime(BigDecimal timeToSafepoint, BigDecimal stoppedTime) {
        if (lastEntry == null) {
            return;
        }
        lastEntry.addTimeToSafepointAndStoppedTime(timeToSafepoint, stoppedTime);
    }

    void addAllData(BigDecimal timeToSafepoint, BigDecimal stoppedTime, BigDecimal applicationTime, String operationName) {
        if (lastEntry == null) {
            return;
        }
        lastEntry.addAllData(timeToSafepoint, stoppedTime, applicationTime, operationName);
    }

    void parsingCompleted() {
        if (lastEntry != null && lastEntry.isCompleted()) {
            safepoints.add(lastEntry);
            lastEntry = null;
        }

        if (safepointOperationStats != null) {
            return;
        }

        safepointOperationStats = SafepointStatsCreator.create(this);
    }
}
