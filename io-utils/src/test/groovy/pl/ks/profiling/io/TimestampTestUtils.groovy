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
package pl.ks.profiling.io

import java.nio.file.Files

class TimestampTestUtils {

    static BigDecimal firstLineTimestamp(File file) {
        try {
            return Files.lines(file.toPath()).map(TimestampTestUtils.&getTimeStamp).findFirst().get();
        } catch (IOException exception) {
            throw new RuntimeException(exception);
        }
    }

    static BigDecimal getTimeStamp(String line) {
        return new BigDecimal(getContentBetweenMarkers(line, "[", "s]"));
    }

    private static String getContentBetweenMarkers(String line, String startMarker, String endMarker) {
        int endMarkerIndex = line.indexOf(endMarker);
        String lineUntilEndMarker = line.substring(0, endMarkerIndex);
        int startMarkerIndex = lineUntilEndMarker.lastIndexOf(startMarker);
        return line.substring(startMarkerIndex + 1, endMarkerIndex);
    }
}
