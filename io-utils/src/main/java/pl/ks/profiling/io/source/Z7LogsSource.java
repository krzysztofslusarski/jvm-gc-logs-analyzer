package pl.ks.profiling.io.source;

import org.apache.commons.compress.archivers.sevenz.SevenZArchiveEntry;
import org.apache.commons.compress.archivers.sevenz.SevenZFile;
import pl.ks.profiling.io.FilesConcatenation;

import java.io.*;
import java.util.List;
import java.util.Scanner;
import java.util.function.Function;
import java.util.stream.Collectors;

public class Z7LogsSource<U extends Comparable<? super U>> extends LogsSourceBase {
    private final File file;

    public Z7LogsSource(File file, Function<String, U> extractCompareObject) throws IOException {
        this.file = file;
        SevenZFile archiveFile = new SevenZFile(file);
        List<SevenZArchiveEntry> entries = (List<SevenZArchiveEntry>) (archiveFile.getEntries());
        this.totalNumberOfFiles = entries.size();
        this.files = getFiles(file, entries);
        this.inputStream = SourceCommons.mergeIntertwined(getSortedEntriesInputStreams(archiveFile, extractCompareObject));
        this.reader = new BufferedReader(new InputStreamReader(getInputStream()));
    }

    private List<LogSourceFile> getFiles(File file, List<SevenZArchiveEntry> entries) {
        return List.of(new LogSourceFile(file.getName(), entries.stream().map(e -> new LogSourceSubfile(e.getName())).collect(Collectors.toList())));
    }

    public static boolean supports(File file) {
        return file.getName().endsWith(".7z");
    }

    @Override
    public String getName() {
        return this.file.getName();
    }

    private List<InputStream> getSortedEntriesInputStreams(SevenZFile archiveFile, Function<String, U> extractCompareObject) {
        List<SevenZArchiveEntry> entries = (List<SevenZArchiveEntry>) (archiveFile.getEntries());
        if (entries.size() == 1) {
            return List.of(new Z7LogsSource.LazySevenZInputStream(archiveFile, entries.get(0), this::startNextFileProcessing));
        }
        List<SevenZArchiveEntry> sorted = FilesConcatenation.sortBy(entries, e -> extractCompareObject.apply(readFirstLine(archiveFile, e)));
        return sorted.stream().map(e -> new Z7LogsSource.LazySevenZInputStream(archiveFile, e, this::startNextFileProcessing)).collect(Collectors.toList());
    }

    private void startNextFileProcessing() {
        this.currentFileNumber++;
    }

    private static String readFirstLine(SevenZFile sevenZFile, SevenZArchiveEntry entry) {
        try {
            return readFirstLine(sevenZFile.getInputStream(entry));
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    private static String readFirstLine(InputStream inputStream) {
        return new Scanner(inputStream).nextLine();
    }

    static class LazySevenZInputStream extends InputStream {
        private final SevenZArchiveEntry entry;
        private final SevenZFile file;
        private InputStream innerInputStream;
        private final Runnable notifyStart;

        public LazySevenZInputStream(SevenZFile file, SevenZArchiveEntry entry, Runnable notifyStart) {
            this.entry = entry;
            this.file = file;
            this.notifyStart = notifyStart;
        }

        @Override
        public int read() throws IOException {
            if (innerInputStream == null) {
                innerInputStream = file.getInputStream(entry);
                notifyStart.run();
            }
            return innerInputStream.read();
        }
    }
}
