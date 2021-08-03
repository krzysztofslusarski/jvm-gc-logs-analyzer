/*
 * Copyright 2020 Krzysztof Slusarski
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

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.util.UUID;
import lombok.experimental.UtilityClass;
import org.apache.poi.util.IOUtils;

@UtilityClass
public class StorageUtils {
    public String createCopy(String dir, String originalFilename, InputStream inputStream) throws IOException {
        String directoryPath = withTrailingSlash(dir);
        makeSureDirectoryExists(directoryPath);
        String savedFileName = directoryPath + UUID.randomUUID().toString() + originalFilename;
        IOUtils.copy(inputStream, new FileOutputStream(savedFileName));
        return savedFileName;
    }

    private void makeSureDirectoryExists(String directoryPath) {
        File directory = new File(directoryPath);
        if (!directory.exists()) {
            directory.mkdirs();
        }
    }

    private static String withTrailingSlash(String path) {
        if (path.endsWith("/")) {
            return path;
        }
        return path + "/";
    }

    public String savePlainText(String dir, String text) throws IOException {
        String savedFileName = dir + "/" + UUID.randomUUID().toString() + "plain-text.log";
        try (PrintWriter out = new PrintWriter(savedFileName)) {
            out.println(text);
        }
        return savedFileName;
    }
}
