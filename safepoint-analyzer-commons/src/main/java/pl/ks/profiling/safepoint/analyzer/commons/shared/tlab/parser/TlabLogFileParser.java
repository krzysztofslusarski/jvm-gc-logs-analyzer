/*
 * Copyright 2020 Krzysztof Slusarski, Artur Owczarek
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

import pl.ks.profiling.safepoint.analyzer.commons.FileParser;
import pl.ks.profiling.safepoint.analyzer.commons.shared.ParserUtils;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class TlabLogFileParser implements FileParser<TlabLogFile> {
    private final TlabLogFile tlabLogFile = new TlabLogFile();
    private final Map<String, String> lastThreadMap = new HashMap<>();
    private final Comparator<ThreadTlabBeforeGC> MostFrequentSlowAllocsFirst = Comparator.comparingLong(ThreadTlabBeforeGC::getSlowAllocs).reversed();

    @Override
    public void parseLine(String line) {
        if (isTlabLine(line)) {
            if (isThreadTlabLine(line)) {
                lastThreadMap.put(parseThreadId(line), line);
            } else if (isTlabSummaryLine(line)) {
                parseTlabSummary(line);
            }
        } else if (gcStarts(line) && !lastThreadMap.isEmpty()) {
            logStatsForThreads();
        }
    }

    private boolean isTlabLine(String line) {
        return line.contains("gc,tlab");
    }

    private void logStatsForThreads() {
        tlabLogFile.newThreadTlabBeforeGc(parseLinesForThreads(lastThreadMap));
        lastThreadMap.clear();
    }

    private boolean gcStarts(String line) {
        return line.contains("gc,start");
    }

    private String parseThreadId(String line) {
        PositionalParser parser = new PositionalParser(line);
        parser.moveAfter("TLAB");
        return parser.readHexadecimalNumber("fill thread");
    }

    private List<ThreadTlabBeforeGC> parseLinesForThreads(Map<String, String> lastThreadMap) {
        return lastThreadMap.entrySet()
                .stream()
                .map(threadEntry -> parseThreadTlabStats(threadEntry.getKey(), threadEntry.getValue()))
                .sorted(MostFrequentSlowAllocsFirst)
                .collect(Collectors.toList());
    }

    private ThreadTlabBeforeGC parseThreadTlabStats(String threadId, String line) {
        PositionalParser parser = new PositionalParser(line);
        parser.moveAfter("TLAB:");
        long nid = parser.readNumericValue("id:");
        long size = parser.readNumericValue("desired_size:");
        long slowAllocs = parser.readNumericValue("slow allocs:");
        return ThreadTlabBeforeGC.builder()
                .tid(threadId)
                .nid(nid)
                .sizeKb(size)
                .slowAllocs(slowAllocs)
                .build();
    }

    private void parseTlabSummary(String line) {
        BigDecimal timeStamp = ParserUtils.getTimeStamp(line);

        PositionalParser parser = new PositionalParser(line);
        parser.moveAfter("TLAB totals:");
        long threadCount = parser.readNumericValue("thrds:");
        long refills = parser.readNumericValue("refills:");
        long maxRefills = parser.readNumericValue("max:");
        long slowAllocs = parser.readNumericValue("slow allocs:");
        long maxSlowAllocs = parser.readNumericValue("max");
        BigDecimal wastePercent = parser.readPercentValue("waste:");

        tlabLogFile.newSummary(timeStamp, threadCount, refills, maxRefills, slowAllocs, maxSlowAllocs, wastePercent);
    }

    private boolean isTlabSummaryLine(String line) {
        // src/hotspot/share/gc/shared/threadLocalAllocBuffer.cpp:453
        return line.contains("TLAB totals"); // isn't better indicator?
//        return line.contains("debug");
    }

    private boolean isThreadTlabLine(String line) {
        // src/hotspot/share/gc/shared/threadLocalAllocBuffer.cpp:283
        return line.contains("trace") && (line.contains("TLAB: fill thread") || line.contains("TLAB: gc thread"));
    }

    @Override
    public TlabLogFile fetchData() {
        return tlabLogFile;
    }

}
