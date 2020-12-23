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
import org.apache.commons.compress.utils.IOUtils;
import org.tukaani.xz.XZInputStream;

import java.io.*;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.zip.GZIPInputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

@UtilityClass
public class InputUtils {
    public static <U extends Comparable<? super U>> InputStream getInputStream(List<File> files, Function<String, U> extractCompareObject) throws IOException {
        if (files.size() == 1) {
            File firstFile = files.get(0);
            String fileName = firstFile.getName();
            String filePath = firstFile.getAbsolutePath();
            if (fileName.endsWith(".7z")) {
                return get7ZipInputStream(filePath);
            } else if (fileName.endsWith(".zip")) {
                return getZipInputStream(filePath);
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
            streamOfFiles = toStreams(FilesConcatenation.sortByFirstLine(files, extractCompareObject));
        } else {
            streamOfFiles = toStreams(files);
        }
        List<InputStream> withNewLinesBetween = intertwineWithNewLineStreams(streamOfFiles);
        return mergeInputStreams(withNewLinesBetween);
    }

    private static InputStream mergeInputStreams(Collection<InputStream> streams) {
        return new SequenceInputStream(new Vector<>(streams).elements());
    }

    private static List<InputStream> intertwineWithNewLineStreams(List<InputStream> inputStreams) {
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

    private static InputStream getZipInputStream(String saveFileName) throws IOException {
        ZipFile zipFile = new ZipFile(saveFileName);
        Enumeration<? extends ZipEntry> entries = zipFile.entries();
        ZipEntry entry = entries.nextElement();

        while (entries.hasMoreElements() && entry.isDirectory()) {
            entry = entries.nextElement();
        }

        InputStream inputStream = null;
        if (entry != null && !entry.isDirectory()) {
            inputStream = zipFile.getInputStream(entry);
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            IOUtils.copy(inputStream, byteArrayOutputStream);
            inputStream = new ByteArrayInputStream(byteArrayOutputStream.toByteArray());
        }

        zipFile.close();
        return inputStream;
    }

    private static InputStream get7ZipInputStream(String saveFileName) throws IOException {
        SevenZFile sevenZFile = new SevenZFile(new File(saveFileName));
        SevenZArchiveEntry entry = sevenZFile.getNextEntry();

        InputStream inputStream = null;
        while (entry != null && entry.isDirectory()) {
            entry = sevenZFile.getNextEntry();
        }

        if (entry != null) {
            byte[] content = new byte[(int) entry.getSize()];
            sevenZFile.read(content, 0, content.length);
            sevenZFile.close();
            inputStream = new ByteArrayInputStream(content);
        }

        sevenZFile.close();
        return inputStream;
    }
}
