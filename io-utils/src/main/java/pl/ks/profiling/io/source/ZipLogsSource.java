package pl.ks.profiling.io.source;

import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipFile;
import pl.ks.profiling.io.FilesConcatenation;

import java.io.*;
import java.util.Collections;
import java.util.List;
import java.util.Scanner;
import java.util.function.Function;
import java.util.stream.Collectors;

public class ZipLogsSource<U extends Comparable<? super U>> extends LogsSourceBase {
    private final File file;

    public ZipLogsSource(File file, Function<String, U> extractCompareObject) throws IOException {
        this.file = file;
        ZipFile archiveFile = new ZipFile(file);
        List<ZipArchiveEntry> entries = Collections.list(archiveFile.getEntries());
        this.files = getFiles(file, entries);
        this.totalNumberOfFiles = entries.size();
        this.inputStream = SourceCommons.mergeIntertwined(getSortedEntriesInputStreams(archiveFile, extractCompareObject));
        this.reader = new BufferedReader(new InputStreamReader(getInputStream()));
    }

    public static boolean supports(File file) {
        return file.getName().endsWith(".zip");
    }

    @Override
    public String getName() {
        return file.getName();
    }

    private List<LogSourceFile> getFiles(File file, List<ZipArchiveEntry> entries) {
        return List.of(new LogSourceFile(file.getName(), entries.stream().map(e -> new LogSourceSubfile(e.getName())).collect(Collectors.toList())));
    }

    private List<InputStream> getSortedEntriesInputStreams(ZipFile archiveFile, Function<String, U> extractCompareObject) {
        List<ZipArchiveEntry> entries = Collections.list(archiveFile.getEntries());
        List<ZipArchiveEntry> sorted = FilesConcatenation.sortBy(entries, e -> extractCompareObject.apply(readFirstLine(archiveFile, e)));
        return sorted.stream().map(e -> new ZipLogsSource.LazyZipInputStream(archiveFile, e, this::startNextFileProcessing)).collect(Collectors.toList());
    }

    private void startNextFileProcessing() {
        this.currentFileNumber++;
    }

    static class LazyZipInputStream extends InputStream {
        private final ZipArchiveEntry entry;
        private final ZipFile file;
        private InputStream innerInputStream;
        private final Runnable notifyStart;

        public LazyZipInputStream(ZipFile file, ZipArchiveEntry entry, Runnable notifyStart) {
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

    private static String readFirstLine(ZipFile archive, ZipArchiveEntry entry) {
        try {
            return readFirstLine(archive.getInputStream(entry));
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    private static String readFirstLine(InputStream inputStream) {
        return new Scanner(inputStream).nextLine();
    }

}
