/*
 * Copyright 2020 Artur Owczarek
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

import org.apache.commons.math3.util.Pair;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Comparator;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

public class FilesConcatenation {
    private static final byte[] NEW_LINE_BYTES = "\n".getBytes();

    public static <U extends Comparable<? super U>> List<File> sortByFirstLine(List<File> files, Function<String, U> extractCompareObject) {
        if (extractCompareObject != null) {
            return sortBySecond(files, extractCompareObjects(files, extractCompareObject));
        } else {
            return files;
        }
    }

    public static void concatenate(List<File> files, File selectedFile, Consumer<ConcatenationProgress> callback) throws IOException {
        FileOutputStream fileOutputStream = new FileOutputStream(selectedFile);
        int numberOfFiles = files.size();
        long allFilesSize = filesSize(files);
        long processedFilesSize = 0;

        int fileNumber = 1;
        if (callback != null) {
            callback.accept(new ConcatenationProgress(
                    fileNumber,
                    numberOfFiles,
                    allFilesSize,
                    processedFilesSize
            ));
        }
        for (File f : files) {
            if (fileNumber > 1) {
                fileOutputStream.write(NEW_LINE_BYTES);
            }
            FileInputStream fileInputStream = new FileInputStream(f);
            fileInputStream.transferTo(fileOutputStream);
            processedFilesSize += f.length();
            if (callback != null) {
                callback.accept(new ConcatenationProgress(
                        fileNumber,
                        numberOfFiles,
                        allFilesSize,
                        processedFilesSize
                ));
            }
            fileNumber++;
        }
    }

    private static <T, U extends Comparable<? super U>> List<T> sortBySecond(List<T> filesList, List<U> orderObjectsList) {
        List<Pair<T, U>> paired = Zipper.zip(filesList, orderObjectsList);
        paired.sort(Comparator.comparing(Pair::getSecond));
        return paired.stream().map(Pair::getFirst).collect(Collectors.toList());
    }

    private static <T> List<T> extractCompareObjects(List<File> filesList, Function<String, T> extractComparisonObject) {
        return filesList.stream().map(file -> extractComparisonObjectForFile(file, extractComparisonObject)).collect(Collectors.toList());
    }

    private static long filesSize(List<File> files) {
        return files.stream().mapToLong(File::length).sum();
    }

    private static <T> T extractComparisonObjectForFile(File file, Function<String, T> extractComparisonObject) {
        return applyOnFirstLine(file, extractComparisonObject);
    }

    private static <T> T applyOnFirstLine(File logsFile, Function<String, T> extractComparisonObject) {
        try {
            return Files.lines(logsFile.toPath()).map(extractComparisonObject).findFirst().get();
        } catch (IOException exception) {
            throw new RuntimeException(exception);
        }
    }
}
