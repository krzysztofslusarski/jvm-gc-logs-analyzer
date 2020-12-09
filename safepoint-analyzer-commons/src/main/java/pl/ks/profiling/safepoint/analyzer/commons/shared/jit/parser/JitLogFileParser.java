package pl.ks.profiling.safepoint.analyzer.commons.shared.jit.parser;

import java.math.BigDecimal;
import pl.ks.profiling.safepoint.analyzer.commons.FileParser;
import pl.ks.profiling.safepoint.analyzer.commons.shared.ParserUtils;

public class JitLogFileParser implements FileParser<JitLogFile> {
    private final JitLogFile jitLogFile = new JitLogFile();

    @Override
    public void parseLine(String line) {
        if (line.contains("codecache,sweep,start")) {
            jitLogFile.newCodeCacheSweeperActivity(ParserUtils.getTimeStamp(line));
        } else if (line.contains("codecache,sweep") && line.contains("size=")) {
            parseCodeCacheStats(line);
        } else if (line.contains("jit,compilation") && line.contains("debug")) {
            parseJitCompilation(line);
        }
    }

    private void parseCodeCacheStats(String line) {
        BigDecimal timeStamp = ParserUtils.getTimeStamp(line);
        String segment = line.substring(line.lastIndexOf(']') + 2);
        segment = segment.substring(0, segment.indexOf(':'));
        long size = parseFirstNumber(line, line.indexOf("size="));
        long used = parseFirstNumber(line, line.indexOf("used="));
        long maxUsed = parseFirstNumber(line, line.indexOf("max_used="));
        jitLogFile.newCodeCacheStats(segment, timeStamp, size, maxUsed, used);
    }

    private static long parseFirstNumber(String line, int pos) {
        boolean started = false;
        long value = 0;
        for (int i = pos; i < line.length(); i++) {
            if (Character.isDigit(line.charAt(i))) {
                started = true;
                value *= 10;
                value += line.charAt(i) - '0';
            } else {
                if (started) {
                    return value;
                }
            }
        }
        return 0;
    }

    private void parseJitCompilation(String line) {
        BigDecimal timeStamp = ParserUtils.getTimeStamp(line);
        int tier = getTier(line);
        if (line.lastIndexOf(')') == line.length() - 1) {
            jitLogFile.newCompilation(timeStamp, tier);
        } else if (line.contains("made not entrant")) {
            jitLogFile.compilationMadeNodEntrant(timeStamp, tier);
        }
    }

    private int getTier(String line) {
        String withoutDecorators = line.substring(line.lastIndexOf(']') + 2);
        boolean flagsStarted = false;
        int countDown = 8;

        for (byte character : withoutDecorators.getBytes()) {
            if (character == ' ') {
                flagsStarted = true;
            }
            if (flagsStarted) {
                countDown--;
            }
            if (countDown == 0) {
                switch (character) {
                    case '1':
                        return 1;
                    case '2':
                        return 2;
                    case '3':
                        return 3;
                    case '4':
                        return 4;
                    default:
                        return 0;
                }
            }
        }
        return 0;
    }

    @Override
    public JitLogFile fetchData() {
        return jitLogFile;
    }
}
