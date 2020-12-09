package pl.ks.profiling.safepoint.analyzer.commons.shared.parser.thread;

import pl.ks.profiling.safepoint.analyzer.commons.FileParser;
import pl.ks.profiling.safepoint.analyzer.commons.shared.ParserUtils;

public class ThreadLogFileParser implements FileParser<ThreadLogFile> {
    private final ThreadLogFile threadLogFile = new ThreadLogFile();

    public ThreadLogFileParser() {
    }

    @Override
    public void parseLine(String line) {
        if (!line.contains("os,thread")) {
            return;
        }

        if (line.contains("Thread started")) {
            threadLogFile.newThreadAdded(ParserUtils.getTimeStamp(line));
        } else if (line.contains("Thread finished")) {
            threadLogFile.threadDestroyed(ParserUtils.getTimeStamp(line));
        }
    }

    @Override
    public ThreadLogFile fetchData() {
        return threadLogFile;
    }
}
