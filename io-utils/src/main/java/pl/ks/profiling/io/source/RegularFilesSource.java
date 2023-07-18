package pl.ks.profiling.io.source;

import pl.ks.profiling.io.FilesConcatenation;

import java.io.*;
import java.nio.file.Files;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

public class RegularFilesSource<U extends Comparable<? super U>> extends LogsSourceBase {
    private final String name;

    public RegularFilesSource(String name, List<File> files, Function<String, U> extractCompareObject) {
        this.name = name;
        this.totalNumberOfFiles = files.size();
        List<File> orderedFiles = FilesConcatenation.sortBy(files, file -> firstLineExtractor(file, extractCompareObject));
        this.files = orderedFiles.stream().map(f -> new LogSourceFile(f.getName(), LogSourceFile.NO_SUBFILES)).collect(Collectors.toList());
        this.inputStream = SourceCommons.mergeIntertwined(toStreams(orderedFiles));
        this.reader = new BufferedReader(new InputStreamReader(getInputStream()));
    }

    @Override
    public String getName() {
        return this.name;
    }

    private List<InputStream> toStreams(List<File> files) {
        return files.stream().map(f -> new LazyFileInputStream(f, this::startNextFileProcessing)).collect(Collectors.toList());
    }

    private void startNextFileProcessing() {
        this.currentFileNumber++;
    }

    static class LazyFileInputStream extends InputStream {
        private final File file;
        private InputStream innerInputStream;
        private final Runnable notifyStart;

        public LazyFileInputStream(File file, Runnable notifyStart) {
            this.file = file;
            this.notifyStart = notifyStart;
        }

        @Override
        public int read() throws IOException {
            if (innerInputStream == null) {
                innerInputStream = new FileInputStream(file);
                notifyStart.run();
            }
            return innerInputStream.read();
        }

        @Override
        public void close() throws IOException {
            this.innerInputStream.close();
        }
    }

    private U firstLineExtractor(File file, Function<String, U> extractCompareObject) {
        try {
            return Files.lines(file.toPath()).map(extractCompareObject).findFirst().get();
        } catch (IOException exception) {
            throw new RuntimeException(exception);
        }
    }
}
