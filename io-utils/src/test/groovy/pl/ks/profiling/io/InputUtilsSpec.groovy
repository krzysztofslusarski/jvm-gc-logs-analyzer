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

import java.util.function.Function

import static pl.ks.profiling.io.TestFileUtils.getFile
import spock.lang.Specification
import java.nio.charset.StandardCharsets

class InputUtilsSpec extends Specification {
    private List<File> files = [
            getFile("loading/file.log.0"),
            getFile("loading/file.log.1"),
            getFile("loading/file.log.2")]

    def "should concatenate files as passed by default"() {
        given:
        InputStream stream = InputUtils.getInputStream(files, Function.identity(), null).inputStream

        when:
        String streamContent = readStream(stream)

        then:
        streamContent == """[2020-12-21T01:04:49.436+0000][30.123s][info ][gc,heap              ] file.log.0

[2020-12-21T01:04:59.827+0000][10.234s][debug][gc,humongous         ] file.log.1
[2020-12-21T01:05:19.416+0000][15.0s][info ][gc,phases            ] file.log.2"""
    }

    def "should sort files before if sorting object function is passed"() {
        given:
        InputStream stream = InputUtils.getInputStream(files, TimestampTestUtils.&getTimeStamp, null).inputStream

        when:
        String streamContent = readStream(stream)

        then:
        streamContent == """[2020-12-21T01:04:59.827+0000][10.234s][debug][gc,humongous         ] file.log.1
[2020-12-21T01:05:19.416+0000][15.0s][info ][gc,phases            ] file.log.2

[2020-12-21T01:04:49.436+0000][30.123s][info ][gc,heap              ] file.log.0"""
    }

    def "should load sorted files from 7z file"() {
        given:
        InputStream stream = InputUtils.getInputStream([getFile("loading/file.log.7z")], TimestampTestUtils.&getTimeStamp, null).inputStream

        when:
        String streamContent = readStream(stream)

        then:
        streamContent == """[2020-12-21T01:04:59.827+0000][10.234s][debug][gc,humongous         ] file.log.1
[2020-12-21T01:05:19.416+0000][15.0s][info ][gc,phases            ] file.log.2

[2020-12-21T01:04:49.436+0000][30.123s][info ][gc,heap              ] file.log.0"""
    }

    def "should load sorted files from zip file"() {
        given:
        InputStream stream = InputUtils.getInputStream([getFile("loading/file.log.zip")], TimestampTestUtils.&getTimeStamp, null).inputStream

        when:
        String streamContent = readStream(stream)

        then:
        streamContent == """[2020-12-21T01:04:59.827+0000][10.234s][debug][gc,humongous         ] file.log.1
[2020-12-21T01:05:19.416+0000][15.0s][info ][gc,phases            ] file.log.2

[2020-12-21T01:04:49.436+0000][30.123s][info ][gc,heap              ] file.log.0"""
    }

    private static String readStream(InputStream stream) {
        return String.join("\n", new InputStreamReader(stream, StandardCharsets.UTF_8).readLines())
    }

}
