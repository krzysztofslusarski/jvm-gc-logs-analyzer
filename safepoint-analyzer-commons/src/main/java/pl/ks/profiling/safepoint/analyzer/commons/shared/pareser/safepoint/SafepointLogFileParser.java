package pl.ks.profiling.safepoint.analyzer.commons.shared.pareser.safepoint;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import pl.ks.profiling.safepoint.analyzer.commons.FileParser;
import pl.ks.profiling.safepoint.analyzer.commons.shared.ParserUtils;

public class SafepointLogFileParser implements FileParser<List<SafepointOperation>> {
    private static final BigDecimal NS_TO_MS_DIVISIOR = new BigDecimal(1_000_000);
    private static final int SCALE = 10;
    private static final String JDK8_REGEX = ".*: .*\\[.*\\] *.*\\[.*\\].*";

    private SafepointOperation current = new SafepointOperation();
    private long seqId = 0;
    private boolean waitForNext = true;
    private boolean jdk8 = false;
    private BigDecimal lastAppTime = null;
    private List<SafepointOperation> safepointOperations = new ArrayList<>();

    @Override
    public void parseLine(String line) {
        if (line.contains("Application time")) {
            current = new SafepointOperation();
            current.setSequenceId(seqId++);
            parseApplicationTime(line, current);
            current.setTimeStamp(ParserUtils.getTimeStamp(line));
            waitForNext = false;
        } else if (!waitForNext && line.contains("Entering safepoint region")) {
            parseRegionName(line, current);
        } else if (!waitForNext && line.contains("Total time for which application threads were stopped")) {
            parseTtsAndStoppedTime(line, current);
            safepointOperations.add(current);
            waitForNext = true;
        } else if (line.contains("Reaching safepoint")) {
            current = new SafepointOperation();
            current.setSequenceId(seqId++);
            parseJava13OneLine(line, current);
            waitForNext = false;
            safepointOperations.add(current);
        } else if (line.contains("vmop") && line.contains("initially_running")) {
            jdk8 = true;
        } else if (jdk8 && line.matches(JDK8_REGEX) && line.split("\\[").length == 3) {
            current = new SafepointOperation();
            current.setSequenceId(seqId++);
            lastAppTime = parseJava8OneLine(line, current, lastAppTime);
            waitForNext = false;
            safepointOperations.add(current);
        }
    }

    @Override
    public List<SafepointOperation> fetchData() {
        return safepointOperations;
    }

    //[1,060s][info][safepoint] Safepoint "Cleanup", Time since last: 1000140224 ns, Reaching safepoint: 196482 ns, At safepoint: 16740 ns, Total: 213222 ns

    private static BigDecimal parseJava8OneLine(String line, SafepointOperation current, BigDecimal lastAppTime) {
        String[] splittedLine = line.split(" ");
        String[] lineNoBlank = Stream.of(splittedLine)
                .filter(s -> s.length() > 0)
                .toArray(String[]::new);
        StringBuilder operationName = new StringBuilder();

        int index = 0;
        BigDecimal currentAppTime = new BigDecimal(lineNoBlank[index++].replaceAll("\\.", "").replaceAll(",", "").replaceAll(":", ""));
        while (!"[".equals(lineNoBlank[index])) {
            operationName.append(lineNoBlank[index]).append(" ");
            index++;
        }

        index++;
        while (!"[".equals(lineNoBlank[index])) {
            index++;
        }

        BigDecimal tts = new BigDecimal(lineNoBlank[index + 3]);
        BigDecimal stoppedTime = new BigDecimal(lineNoBlank[index + 5]).add(tts);
        current.setApplicationTime(lastAppTime == null ? BigDecimal.ZERO : currentAppTime.subtract(lastAppTime).subtract(stoppedTime)
                .divide(SafepointStatsCreator.TO_MS_MULTIPLIER, SCALE, RoundingMode.HALF_EVEN));
        current.setOperationName(operationName.toString().trim());
        current.setStoppedTime(stoppedTime.divide(SafepointStatsCreator.TO_MS_MULTIPLIER, SCALE, RoundingMode.HALF_EVEN));
        current.setTtsTime(tts.divide(SafepointStatsCreator.TO_MS_MULTIPLIER, SCALE, RoundingMode.HALF_EVEN));

        return currentAppTime;
    }

    private static void parseJava13OneLine(String line, SafepointOperation current) {
        String name = line
                .replaceFirst(".* Safepoint \"", "")
                .replaceFirst("\", Time since.*", "")
                .trim();
        String tts = line
                .replaceFirst(".* Reaching safepoint: ", "")
                .replaceFirst(" ns, At safepoint.*", "")
                .trim();
        String stopped = line
                .replaceFirst(".*Total: ", "")
                .replaceFirst(" ns", "")
                .trim();
        String appTime = line
                .replaceFirst(".*Time since last: ", "")
                .replaceFirst(" ns, Reaching safepoint.*", "")
                .trim();
        current.setApplicationTime(new BigDecimal(appTime)
                .divide(NS_TO_MS_DIVISIOR, SCALE, RoundingMode.HALF_EVEN)
                .divide(SafepointStatsCreator.TO_MS_MULTIPLIER, SCALE, RoundingMode.HALF_EVEN));
        current.setOperationName(name);
        current.setStoppedTime(new BigDecimal(stopped)
                .divide(NS_TO_MS_DIVISIOR, SCALE, RoundingMode.HALF_EVEN)
                .divide(SafepointStatsCreator.TO_MS_MULTIPLIER, SCALE, RoundingMode.HALF_EVEN));
        current.setTtsTime(new BigDecimal(tts)
                .divide(NS_TO_MS_DIVISIOR, SCALE, RoundingMode.HALF_EVEN)
                .divide(SafepointStatsCreator.TO_MS_MULTIPLIER, SCALE, RoundingMode.HALF_EVEN));
    }

    private static void parseTtsAndStoppedTime(String line, SafepointOperation current) {
        String tts = line
                .replaceFirst(".*Stopping threads took: ", "")
                .replace(" seconds", "")
                .replace(',', '.')
                .trim();
        String stopped = line
                .replaceFirst(".*Total time for which application threads were stopped: ", "")
                .replaceFirst(" seconds, Stopping threads.*", "")
                .replace(',', '.')
                .trim();

        current.setStoppedTime(new BigDecimal(stopped));
        current.setTtsTime(new BigDecimal(tts));
    }

    private static void parseRegionName(String line, SafepointOperation current) {
        String name = line
                .replaceFirst(".*Entering safepoint region: ", "")
                .trim();
        current.setOperationName(name);
    }

    private static void parseApplicationTime(String line, SafepointOperation current) {
        String appTime = line
                .replaceFirst(".*Application time: ", "")
                .replace(" seconds", "")
                .replace(',', '.')
                .trim();
        current.setApplicationTime(new BigDecimal(appTime));
    }

    public static List<SafepointOperation> parse(InputStream inputStream) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
        SafepointLogFileParser safepointLogFileParser = new SafepointLogFileParser();
        while (reader.ready()) {
            String line = reader.readLine();
            safepointLogFileParser.parseLine(line);
        }

        return safepointLogFileParser.fetchData();
    }
}
