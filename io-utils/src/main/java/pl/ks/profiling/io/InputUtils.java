/*
 * Copyright 2020 Krzysztof Slusarski, Artur Owczarek
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package pl.ks.profiling.io;

import lombok.experimental.UtilityClass;
import org.apache.commons.compress.archivers.sevenz.SevenZArchiveEntry;
import org.apache.commons.compress.archivers.sevenz.SevenZFile;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipFile;
import org.apache.commons.compress.utils.IOUtils;
import org.tukaani.xz.XZInputStream;

import java.io.*;
import java.nio.file.Files;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.zip.GZIPInputStream;

@UtilityClass
public class InputUtils {
    public static <U extends Comparable<? super U>> InputStream getInputStream(List<File> files, Function<String, U> extractCompareObject) throws IOException {
        if (files.size() == 1) {
            File firstFile = files.get(0);
            String fileName = firstFile.getName();
            String filePath = firstFile.getAbsolutePath();
            if (fileName.endsWith(".7z")) {
                return get7ZipInputStream(firstFile, extractCompareObject);
            } else if (fileName.endsWith(".zip")) {
                return getZipInputStream(filePath, extractCompareObject);
            } else if (fileName.endsWith(".xz")) {
                return getXZInputStream(filePath);
            } else if (fileName.endsWith(".gz") || fileName.endsWith(".gzip")) {
                return getGZipInputStream(filePath);
            }
        }
        return getConcatenatedInputStream(files, extractCompareObject);
    }

    private static <U extends Comparable<? super U>> InputStream getConcatenatedInputStream(List<File> files, Function<String, U> extractCompareObject) {
        List<InputStream> streamOfFiles;
        if (extractCompareObject != null) {
            streamOfFiles = toStreams(FilesConcatenation.sortBy(files, file -> firstLineExtractor(file, extractCompareObject)));
        } else {
            streamOfFiles = toStreams(files);
        }
        return mergeIntertwined(streamOfFiles);
    }

    private static InputStream mergeIntertwined(Collection<InputStream> streams) {
        List<InputStream> withNewLinesBetween = intertwineWithNewLineStreams(streams);
        return mergeInputStreams(withNewLinesBetween);
    }

    private static InputStream mergeInputStreams(Collection<InputStream> streams) {
        return new SequenceInputStream(new Vector<>(streams).elements());
    }

    private static List<InputStream> intertwineWithNewLineStreams(Collection<InputStream> inputStreams) {
        int numberOfStreams = inputStreams.size();
        int numberOfNewLinesBetween = numberOfStreams - 1;
        List<InputStream> result = new ArrayList<>(numberOfStreams + numberOfNewLinesBetween);
        int lastStreamIndex = numberOfStreams - 1;
        int currentStreamIndex = 0;
        for (InputStream stream : inputStreams) {
            result.add(stream);
            if (currentStreamIndex < lastStreamIndex) {
                result.add(newLineInputStream());
            }
            currentStreamIndex++;
        }

        return result;
    }

    private static InputStream newLineInputStream() {
        return new ByteArrayInputStream("\n".getBytes());
    }

    private static List<InputStream> toStreams(List<File> files) {
        return files.stream().map(w -> {
            try {
                return new FileInputStream(w);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
                throw new RuntimeException(e);
            }
        }).collect(Collectors.toList());
    }

    private static InputStream getXZInputStream(String saveFileName) throws IOException {
        XZInputStream xzInputStream = new XZInputStream(new FileInputStream(saveFileName));
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        IOUtils.copy(xzInputStream, byteArrayOutputStream);
        xzInputStream.close();
        return new ByteArrayInputStream(byteArrayOutputStream.toByteArray());
    }

    private static InputStream getGZipInputStream(String saveFileName) throws IOException {
        GZIPInputStream gzipInputStream = new GZIPInputStream(new FileInputStream(saveFileName));
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        IOUtils.copy(gzipInputStream, byteArrayOutputStream);
        gzipInputStream.close();
        return new ByteArrayInputStream(byteArrayOutputStream.toByteArray());
    }

    private static <U extends Comparable<? super U>> InputStream getZipInputStream(String saveFileName, Function<String, U> extractCompareObject) throws IOException {
        ZipFile zipFile = new ZipFile(saveFileName);
        return mergeIntertwined(getSortedEntriesInputStreams(zipFile, extractCompareObject));
    }

    public static <U extends Comparable<? super U>> InputStream get7ZipInputStream(File file, Function<String, U> extractCompareObject) throws IOException {
        SevenZFile sevenZFile = new SevenZFile(file);
        return mergeIntertwined(getSortedEntriesInputStreams(sevenZFile, extractCompareObject));
    }

    private static <U extends Comparable<? super U>> List<InputStream> getSortedEntriesInputStreams(ZipFile archiveFile, Function<String, U> extractCompareObject) {
        List<ZipArchiveEntry> entries = Collections.list(archiveFile.getEntries());
        List<ZipArchiveEntry> sorted = FilesConcatenation.sortBy(entries, e -> extractCompareObject.apply(readFirstLine(archiveFile, e)));
        return sorted.stream().map(e -> new LazyZipInputStream(archiveFile, e)).collect(Collectors.toList());
    }

    private static <U extends Comparable<? super U>> List<InputStream> getSortedEntriesInputStreams(SevenZFile archiveFile, Function<String, U> extractCompareObject) {
        List<SevenZArchiveEntry> entries = (List<SevenZArchiveEntry>) (archiveFile.getEntries());
        List<SevenZArchiveEntry> sorted = FilesConcatenation.sortBy(entries, e -> extractCompareObject.apply(readFirstLine(archiveFile, e)));
        return sorted.stream().map(e -> new LazySevenZInputStream(archiveFile, e)).collect(Collectors.toList());
    }

    static class LazySevenZInputStream extends InputStream {
        private final SevenZArchiveEntry entry;
        private final SevenZFile file;
        private InputStream innerInputStream;

        public LazySevenZInputStream(SevenZFile file, SevenZArchiveEntry entry) {
            this.entry = entry;
            this.file = file;
        }

        @Override
        public int read() throws IOException {
            if (innerInputStream == null) {
                innerInputStream = file.getInputStream(entry);
            }
            return innerInputStream.read();
        }
    }

    static class LazyZipInputStream extends InputStream {
        private final ZipArchiveEntry entry;
        private final ZipFile file;
        private InputStream innerInputStream;

        public LazyZipInputStream(ZipFile file, ZipArchiveEntry entry) {
            this.entry = entry;
            this.file = file;
        }

        @Override
        public int read() throws IOException {
            if (innerInputStream == null) {
                innerInputStream = file.getInputStream(entry);
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

    private static <U extends Comparable<? super U>> U firstLineExtractor(File file, Function<String, U> extractCompareObject) {
        try {
            return Files.lines(file.toPath()).map(extractCompareObject).findFirst().get();
        } catch (IOException exception) {
            throw new RuntimeException(exception);
        }
    }
}
