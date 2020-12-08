package pl.ks.profiling.safepoint.analyzer.commons.shared.pareser.stringdedup;

import java.math.BigDecimal;
import pl.ks.profiling.safepoint.analyzer.commons.FileParser;
import pl.ks.profiling.safepoint.analyzer.commons.shared.ParserUtils;

public class StringDedupLogFileParser implements FileParser<StringDedupLogFile> {
    public static final BigDecimal MB = new BigDecimal(1024);
    private final StringDedupLogFile logFile = new StringDedupLogFile();

    @Override
    public void parseLine(String line) {
        if (!line.contains("gc,stringdedup")) {
            return;
        }

        if (line.contains("Concurrent String Deduplication")) {
            logFile.newEntry(ParserUtils.getTimeStamp(line));
        } else if (line.contains("Last Exec")) {
            logFile.lastExecOccurred();
        } else if (line.contains("Total Exec")) {
            logFile.totalExecOccurred();
        } else if (line.contains("Deduplicated")) {
            int pos = line.indexOf("Deduplicated");
            long count = ParserUtils.parseFirstNumber(line, pos);
            int percentPos = line.indexOf("%)", pos);
            BigDecimal size = ParserUtils.parseFirstBigDecimal(line, percentPos);
            if (line.indexOf("M(", percentPos) >= 0) {
                size = size.multiply(MB);
            } else if (line.indexOf("G(", percentPos) >= 0) {
                size = size.multiply(MB).multiply(MB);
            }
            logFile.deduplicated(count, size);
        } else if (line.contains("Young")) {
            int pos = line.indexOf("Young");
            long count = ParserUtils.parseFirstNumber(line, pos);
            int percentPos = line.indexOf("%)", pos);
            BigDecimal size = ParserUtils.parseFirstBigDecimal(line, percentPos);
            if (line.indexOf("M(", percentPos) >= 0) {
                size = size.multiply(MB);
            } else if (line.indexOf("G(", percentPos) >= 0) {
                size = size.multiply(MB).multiply(MB);
            }
            logFile.young(count, size);
        } else if (line.contains("Old")) {
            int pos = line.indexOf("Old");
            long count = ParserUtils.parseFirstNumber(line, pos);
            int percentPos = line.indexOf("%)", pos);
            BigDecimal size = ParserUtils.parseFirstBigDecimal(line, percentPos);
            if (line.indexOf("M(", percentPos) >= 0) {
                size = size.multiply(MB);
            } else if (line.indexOf("G(", percentPos) >= 0) {
                size = size.multiply(MB).multiply(MB);
            }
            logFile.old(count, size);
        } else if (line.contains("New")) {
            int pos = line.indexOf("New");
            long count = ParserUtils.parseFirstNumber(line, pos);
            int percentPos = line.indexOf("%)", pos);
            BigDecimal size = ParserUtils.parseFirstBigDecimal(line, percentPos);
            if (line.indexOf("M", percentPos) >= 0) {
                size = size.multiply(MB);
            } else if (line.indexOf("G", percentPos) >= 0) {
                size = size.multiply(MB).multiply(MB);
            }
            logFile.newStrings(count, size);
        }
    }

    @Override
    public StringDedupLogFile fetchData() {
        return logFile;
    }
}
