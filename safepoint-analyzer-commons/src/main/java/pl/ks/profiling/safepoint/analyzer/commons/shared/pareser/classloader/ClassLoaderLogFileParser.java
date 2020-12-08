package pl.ks.profiling.safepoint.analyzer.commons.shared.pareser.classloader;

import pl.ks.profiling.safepoint.analyzer.commons.FileParser;
import pl.ks.profiling.safepoint.analyzer.commons.shared.ParserUtils;

public class ClassLoaderLogFileParser implements FileParser<ClassLoaderLogFile> {
    private final ClassLoaderLogFile classLoaderLogFile = new ClassLoaderLogFile();

    @Override
    public void parseLine(String line) {
        if (line.contains("class,load") && line.contains("info")) {
            classLoaderLogFile.newClassLoaded(ParserUtils.getTimeStamp(line));
        } else if (line.contains("class,unload") && line.contains("unloading class")) {
            classLoaderLogFile.classUnloaded(ParserUtils.getTimeStamp(line));
        }
    }

    @Override
    public ClassLoaderLogFile fetchData() {
        return classLoaderLogFile;
    }
}
