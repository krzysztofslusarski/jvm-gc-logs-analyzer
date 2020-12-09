package pl.ks.profiling.safepoint.analyzer.commons.shared.tlab.parser;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;
import pl.ks.profiling.safepoint.analyzer.commons.FileParser;
import pl.ks.profiling.safepoint.analyzer.commons.shared.ParserUtils;

public class TlabLogFileParser implements FileParser<TlabLogFile> {
    private final TlabLogFile tlabLogFile = new TlabLogFile();
    private final Map<String, String> lastThreadMap = new HashMap<>();

    @Override
    public void parseLine(String line) {
        if (line.contains("gc,start") && !lastThreadMap.isEmpty()) {
            tlabLogFile.newThreadTlabBeforeGc(lastThreadMap.entrySet().stream()
                    .map(entry -> ThreadTlabBeforeGC.builder()
                            .tid(entry.getKey())
                            .nid(ParserUtils.parseFirstNumber(entry.getValue(), entry.getValue().indexOf("id:")))
                            .size(ParserUtils.parseFirstNumber(entry.getValue(), entry.getValue().indexOf("desired_size:")))
                            .slowAllocs(ParserUtils.parseFirstNumber(entry.getValue(), entry.getValue().indexOf("slow allocs:")))
                            .build())
                    .sorted(Comparator.comparingLong(ThreadTlabBeforeGC::getSlowAllocs).reversed())
                    .collect(Collectors.toList())
            );
            lastThreadMap.clear();
            return;
        }

        if (!line.contains("gc,tlab")) {
            return;
        }

        if (line.contains("debug")) {
            parseSummary(line);
        } else if (line.contains("trace") && line.contains("fill thread")) {
            int beginIndex = line.indexOf("0x");
            String tid = line.substring(beginIndex, beginIndex + 18);
            lastThreadMap.put(tid, line);
        }
    }

    private void parseSummary(String line) {
        BigDecimal timeStamp = ParserUtils.getTimeStamp(line);
        long threadCount = ParserUtils.parseFirstNumber(line, line.indexOf("thrds:"));
        long refills = ParserUtils.parseFirstNumber(line, line.indexOf("refills:"));
        long maxRefills = ParserUtils.parseFirstNumber(line, line.indexOf("max:"));
        int slowAllocsPos = line.indexOf("slow allocs:");
        long slowAllocs = ParserUtils.parseFirstNumber(line, slowAllocsPos);
        long maxSlowAllocs = ParserUtils.parseFirstNumber(line, line.indexOf("max", slowAllocsPos));
        BigDecimal wastePercent = ParserUtils.parseFirstBigDecimal(line, line.indexOf("waste:"));
        tlabLogFile.newSummary(timeStamp, threadCount, refills, maxRefills, slowAllocs, maxSlowAllocs, wastePercent);
    }


    @Override
    public TlabLogFile fetchData() {
        return tlabLogFile;
    }

}
