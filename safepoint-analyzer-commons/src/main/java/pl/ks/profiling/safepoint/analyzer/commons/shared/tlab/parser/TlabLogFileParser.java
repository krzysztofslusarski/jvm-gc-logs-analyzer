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
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class TlabLogFileParser implements FileParser<TlabLogFile> {
    private static final Pattern ZGC_COLLECTION_START_PATTERN =
            // The start line ends with the cause, which may contain parentheses: "(System.gc())"
            Pattern.compile("\\[gc *\\] GC\\(\\d+\\) (Major|Minor) Collection \\(.*\\)\\s*$");
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

    /**
     * Start of a collection. Generational ZGC (JDK 21+) does not log "gc,start", its cycles start with
     * "GC(3) Major Collection (Warmup)" / "GC(4) Minor Collection (Allocation Rate)".
     */
    private boolean gcStarts(String line) {
        return line.contains("gc,start") || ZGC_COLLECTION_START_PATTERN.matcher(line).find();
    }

    /**
     * The thread address: "TLAB: fill thread: 0x...", "TLAB: gc thread: 0x..." (JDK 9-26),
     * "TLAB fill: thread: 0x...", "TLAB GC: thread: 0x..." (JDK 27+).
     */
    private String parseThreadId(String line) {
        return ParserUtils.parseFirstHexadecimalNumber(line, line.indexOf("thread: 0x"));
    }

    private List<ThreadTlabBeforeGC> parseLinesForThreads(Map<String, String> lastThreadMap) {
        return lastThreadMap.entrySet()
                .stream()
                .map(threadEntry -> parseThreadTlabStats(threadEntry.getKey(), threadEntry.getValue()))
                .sorted(MostFrequentSlowAllocsFirst)
                .collect(Collectors.toList());
    }

    private ThreadTlabBeforeGC parseThreadTlabStats(String threadId, String line) {
        long nid = numberAfter(line, "[id:");
        // JDK 9-26: "desired_size: 632KB"; JDK 27+: "desired: 96K" (GC line) or "capacity: 96K" (fill line)
        long size = numberAfter(line, "desired_size:", "desired:", "capacity:");
        long slowAllocs = numberAfter(line, "slow allocs:");
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
        return line.contains("TLAB totals");
    }

    private boolean isThreadTlabLine(String line) {
        // src/hotspot/share/gc/shared/threadLocalAllocBuffer.cpp:283 (the format changed in JDK 27)
        return line.contains("trace") && (line.contains("TLAB: fill thread") || line.contains("TLAB: gc thread")
                || line.contains("TLAB fill: thread") || line.contains("TLAB GC: thread"));
    }

    /**
     * The number after the first of the markers present in the line, 0 if none is present.
     */
    private static long numberAfter(String line, String... markers) {
        for (String marker : markers) {
            int position = line.indexOf(marker);
            if (position >= 0) {
                return ParserUtils.parseFirstNumber(line, position + marker.length());
            }
        }
        return 0;
    }

    @Override
    public TlabLogFile fetchData() {
        return tlabLogFile;
    }

}
