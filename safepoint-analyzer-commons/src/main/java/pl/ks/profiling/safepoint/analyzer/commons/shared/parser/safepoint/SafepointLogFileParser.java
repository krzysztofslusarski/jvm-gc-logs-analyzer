package pl.ks.profiling.safepoint.analyzer.commons.shared.parser.safepoint;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.stream.Stream;
import pl.ks.profiling.safepoint.analyzer.commons.FileParser;
import pl.ks.profiling.safepoint.analyzer.commons.shared.ParserUtils;

public class SafepointLogFileParser implements FileParser<SafepointLogFile> {
    private static final BigDecimal NS_TO_MS_DIVISIOR = new BigDecimal(1_000_000);
    private static final int SCALE = 10;
    private static final String JDK8_REGEX = ".*: .*\\[.*\\] *.*\\[.*\\].*";

    private boolean waitForNext = true;
    private boolean jdk8 = false;
    private BigDecimal lastAppTime = null;

    private SafepointLogFile safepointLogFile = new SafepointLogFile();

    @Override
    public void parseLine(String line) {
        if (line.contains("Application time")) {
            safepointLogFile.newSafepoint(ParserUtils.getTimeStamp(line), parseApplicationTime(line));
            waitForNext = false;
        } else if (!waitForNext && line.contains("Entering safepoint region")) {
            addOperationName(line, safepointLogFile);
        } else if (!waitForNext && line.contains("Total time for which application threads were stopped")) {
            addTtsAndStoppedTime(line, safepointLogFile);
            waitForNext = true;
        } else if (line.contains("Reaching safepoint")) {
            safepointLogFile.newSafepoint(ParserUtils.getTimeStamp(line));
            addJava13OneLine(line, safepointLogFile);
            waitForNext = false;
        } else if (line.contains("vmop") && line.contains("initially_running")) {
            jdk8 = true;
        } else if (jdk8 && line.matches(JDK8_REGEX) && line.split("\\[").length == 3) {
            safepointLogFile.newSafepoint(ParserUtils.getTimeStamp(line));
            lastAppTime = addJava8OneLine(line, safepointLogFile, lastAppTime);
            waitForNext = false;
        }
    }

    @Override
    public SafepointLogFile fetchData() {
        safepointLogFile.parsingCompleted();
        return safepointLogFile;
    }

    //[1,060s][info][safepoint] Safepoint "Cleanup", Time since last: 1000140224 ns, Reaching safepoint: 196482 ns, At safepoint: 16740 ns, Total: 213222 ns

    private static BigDecimal addJava8OneLine(String line, SafepointLogFile current, BigDecimal lastAppTime) {
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
        current.addAllData(
                tts.divide(SafepointStatsCreator.TO_MS_MULTIPLIER, SCALE, RoundingMode.HALF_EVEN),
                stoppedTime.divide(SafepointStatsCreator.TO_MS_MULTIPLIER, SCALE, RoundingMode.HALF_EVEN),
                lastAppTime == null ? BigDecimal.ZERO : currentAppTime.subtract(lastAppTime).subtract(stoppedTime)
                        .divide(SafepointStatsCreator.TO_MS_MULTIPLIER, SCALE, RoundingMode.HALF_EVEN),
                operationName.toString().trim()
        );

        return currentAppTime;
    }

    private static void addJava13OneLine(String line, SafepointLogFile current) {
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
        current.addAllData(
                new BigDecimal(tts)
                        .divide(NS_TO_MS_DIVISIOR, SCALE, RoundingMode.HALF_EVEN)
                        .divide(SafepointStatsCreator.TO_MS_MULTIPLIER, SCALE, RoundingMode.HALF_EVEN),
                new BigDecimal(stopped)
                        .divide(NS_TO_MS_DIVISIOR, SCALE, RoundingMode.HALF_EVEN)
                        .divide(SafepointStatsCreator.TO_MS_MULTIPLIER, SCALE, RoundingMode.HALF_EVEN),
                new BigDecimal(appTime)
                        .divide(NS_TO_MS_DIVISIOR, SCALE, RoundingMode.HALF_EVEN)
                        .divide(SafepointStatsCreator.TO_MS_MULTIPLIER, SCALE, RoundingMode.HALF_EVEN),
                name
        );
    }

    private static void addTtsAndStoppedTime(String line, SafepointLogFile current) {
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

        current.addTimeToSafepointAndStoppedTime(new BigDecimal(tts), new BigDecimal(stopped));
    }

    private static void addOperationName(String line, SafepointLogFile safepointLogFile) {
        String name = line
                .replaceFirst(".*Entering safepoint region: ", "")
                .trim();
        safepointLogFile.addOperationName(name);
    }

    private static BigDecimal parseApplicationTime(String line) {
        String appTime = line
                .replaceFirst(".*Application time: ", "")
                .replace(" seconds", "")
                .replace(',', '.')
                .trim();
        return new BigDecimal(appTime);
    }
}
