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
import pl.ks.profiling.io.source.*;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.function.Function;

@UtilityClass
public class InputUtils {
    private static final String USE_FIRST_FILE_NAME = null;

    public static <U extends Comparable<? super U>> LogsSource getLogsSource(
            List<File> files,
            Function<String, U> extractCompareObject) throws IOException {
        return InputUtils.getInputStream(files, extractCompareObject, USE_FIRST_FILE_NAME);
    }

    public static <U extends Comparable<? super U>> LogsSource getLogsSource(
            String savedLocation,
            String name,
            Function<String, U> extractCompareObject) throws IOException {
        return InputUtils.getInputStream(List.of(new File(savedLocation)), extractCompareObject, name);
    }

    public static <U extends Comparable<? super U>> LogsSource getInputStream(
            List<File> files,
            Function<String, U> extractCompareObject,
            String overrideName) throws IOException {
        File firstFile = files.get(0);
        if (files.size() == 1) {
            if (Z7LogsSource.supports(firstFile)) {
                return new Z7LogsSource<>(firstFile, extractCompareObject);
            } else if (ZipLogsSource.supports(firstFile)) {
                return new ZipLogsSource<>(firstFile, extractCompareObject);
            } else if (XZSource.supports(firstFile)) {
                return new XZSource(firstFile);
            } else if (GZipInputSource.supports(firstFile)) {
                return new GZipInputSource(firstFile);
            }
        }

        String name = overrideName != null ? overrideName : firstFile.getName();
        return new RegularFilesSource<>(name, files, extractCompareObject);
    }
}
