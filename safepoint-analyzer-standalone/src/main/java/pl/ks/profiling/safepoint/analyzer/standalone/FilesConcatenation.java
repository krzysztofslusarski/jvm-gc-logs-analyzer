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
package pl.ks.profiling.safepoint.analyzer.standalone;

import org.apache.commons.math3.util.Pair;
import pl.ks.profiling.safepoint.analyzer.commons.shared.ParserUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.util.*;
import java.util.stream.Collectors;

public class FilesConcatenation {
    private static final byte[] NEW_LINE_BYTES = "\n".getBytes();

    public static List<File> sortByTimestamp(File[] files) throws Exception {
        List<File> filesList = Arrays.asList(files);
        List<BigDecimal> firstLinesTimestamps = filesTimestamps(filesList);
        return sortBySecond(filesList, firstLinesTimestamps);
    }

    private static <T> List<T> sortBySecond(List<T> filesList, List<BigDecimal> orderObjectsList) {
        List<Pair<T, BigDecimal>> zip = zip(filesList, orderObjectsList);
        zip.sort(Comparator.comparing(Pair::getSecond));
        return zip.stream().map(Pair::getFirst).collect(Collectors.toList());
    }

    private static <T, U> List<Pair<T, U>> zip(List<T> ts, List<U> us) {
        Iterator<T> t = ts.iterator();
        Iterator<U> u = us.iterator();
        List<Pair<T, U>> zipped = new ArrayList<>(Math.min(ts.size(), us.size()));
        while (t.hasNext() && u.hasNext()) {
            zipped.add(new Pair<T, U>(t.next(), u.next()));
        }
        return zipped;
    }

    private static List<BigDecimal> filesTimestamps(List<File> filesList) {
        return filesList.stream().map(FilesConcatenation::getFirstRowTimestamp).collect(Collectors.toList());
    }

    public static void concatenate(List<File> files, File selectedFile) throws IOException {
        FileOutputStream fileOutputStream = new FileOutputStream(selectedFile);
        int fileNumber = 0;
        for (File f : files) {
            if (fileNumber > 0) {
                fileOutputStream.write(NEW_LINE_BYTES);
            }
            FileInputStream fileInputStream = new FileInputStream(f);
            fileInputStream.transferTo(fileOutputStream);
            fileNumber++;
        }
    }

    private static BigDecimal getFirstRowTimestamp(File logsFile) {
        try {
            return Files.lines(logsFile.toPath()).map(ParserUtils::getTimeStamp).findFirst().get();
        } catch (IOException exception) {
            throw new RuntimeException(exception);
        }
    }
}
