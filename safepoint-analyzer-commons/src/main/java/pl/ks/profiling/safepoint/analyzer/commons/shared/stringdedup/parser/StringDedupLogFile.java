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
package pl.ks.profiling.safepoint.analyzer.commons.shared.stringdedup.parser;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;

public class StringDedupLogFile {
    @Getter
    private final List<StringDedupLogEntry> entries = new ArrayList<>();
    private StringDedupLogEntry lastEntry = new StringDedupLogEntry();

    private Stage stage = Stage.UNKNOWN;

    void newEntry(BigDecimal timestamp) {
        if (lastEntry.isInitialized()) {
            entries.add(lastEntry);
            lastEntry = new StringDedupLogEntry();
        }
        stage = Stage.UNKNOWN;
        lastEntry.setTimeStamp(timestamp);
    }

    void lastExecOccurred() {
        stage = Stage.LAST_EXEC;
    }

    void totalExecOccurred() {
        stage = Stage.TOTAL_EXEC;
    }

    void deduplicated(long count, BigDecimal size) {
        switch (stage) {
            case LAST_EXEC:
                lastEntry.deduplicatedLast(count, size);
                break;
            case TOTAL_EXEC:
                lastEntry.deduplicatedTotal(count, size);
                break;
        }
    }

    void young(long count, BigDecimal size) {
        switch (stage) {
            case LAST_EXEC:
                lastEntry.youngLast(count, size);
                break;
            case TOTAL_EXEC:
                lastEntry.youngTotal(count, size);
                break;
        }
    }

    void old(long count, BigDecimal size) {
        switch (stage) {
            case LAST_EXEC:
                lastEntry.oldLast(count, size);
                break;
            case TOTAL_EXEC:
                lastEntry.oldTotal(count, size);
                break;
        }
    }

    public void newStrings(long count, BigDecimal size) {
        switch (stage) {
            case LAST_EXEC:
                lastEntry.newLast(count, size);
                break;
            case TOTAL_EXEC:
                lastEntry.newTotal(count, size);
                break;
        }
    }
}
