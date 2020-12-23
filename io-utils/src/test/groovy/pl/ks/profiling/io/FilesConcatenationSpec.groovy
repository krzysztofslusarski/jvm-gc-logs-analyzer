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

import spock.lang.Specification

import java.nio.file.Files
import java.util.stream.Collectors

class FilesConcatenationSpec extends Specification {

    def "should sort files with first timestamp in file"() {
        given:
        File file1 = getFile("concatenation/concatFile.3.log")
        File file2 = getFile("concatenation/concatFile.4.log")
        File file3 = getFile("concatenation/concatFile.0.log")
        File file4 = getFile("concatenation/concatFile.1.log")
        File file5 = getFile("concatenation/concatFile.2.log")
        File file6 = getFile("concatenation/concatFile.log")

        def approach1 = [file1, file2, file3, file4, file5, file6]
        def approach2 = [file6, file5, file4, file3, file2, file1]
        def approach3 = [file6, file5, file4, file3, file2, file1]

        when:
        def sorted1 = FilesConcatenation.sortByFirstLine(approach1, TimestampTestUtils.&getTimeStamp)
        def sorted2 = FilesConcatenation.sortByFirstLine(approach2, TimestampTestUtils.&getTimeStamp)
        def sorted3 = FilesConcatenation.sortByFirstLine(approach3, TimestampTestUtils.&getTimeStamp)

        then:
        sorted1[0] == file1
        sorted1[1] == file2
        sorted1[2] == file3
        sorted1[3] == file4
        sorted1[4] == file5
        sorted1[5] == file6

        sorted2[0] == file1
        sorted2[1] == file2
        sorted2[2] == file3
        sorted2[3] == file4
        sorted2[4] == file5
        sorted2[5] == file6

        sorted3[0] == file1
        sorted3[1] == file2
        sorted3[2] == file3
        sorted3[3] == file4
        sorted3[4] == file5
        sorted3[5] == file6
    }

    def "should concatenate files"() {
        when:
        String concatenated = concatenateFiles("fileWithoutNewLine.log", "nextFile.log")

        then:
        concatenated == """file without new line
some next file"""
    }

    def "should keep new lines while concatenating"() {
        when:
        String concatenated = concatenateFiles("fileWithNewLine.log", "nextFile.log")

        then:
        concatenated == """file with new line

some next file"""
    }

    private static File getFile(String filePath) {
        return new File(FilesConcatenationSpec.class.getClassLoader().getResource(filePath).toURI())
    }

    private static File createTemporaryFile() {
        File file = File.createTempFile("jvm-gc-logs-analyzer-tests-", "-filesConcatenationSpec.tmp")
        file.deleteOnExit()
        return file;
    }

    private static String concatenateFiles(String... filesPath) {
        File outputFile = createTemporaryFile()
        List<File> files = (filesPath as List<String>).collect{getFile(it)}
        FilesConcatenation.concatenate(files, outputFile, null)
        return Files.lines(outputFile.toPath()).collect(Collectors.toList()).join("\n")
    }
}
