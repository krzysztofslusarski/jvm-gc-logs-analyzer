
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
package pl.ks.profiling.safepoint.analyzer.standalone

import spock.lang.Specification

class FilesConcatenationSpec extends Specification {
    def "Should sort files with first timestamp in file"() {
        given:
        File file1 = getFile("./concatFile.3.log")
        File file2 = getFile("./concatFile.4.log")
        File file3 = getFile("./concatFile.0.log")
        File file4 = getFile("./concatFile.1.log")
        File file5 = getFile("./concatFile.2.log")
        File file6 = getFile("./concatFile.log")

        def approach1 = [file1, file2, file3, file4, file5, file6] as File[]
        def approach2 = [file6, file5, file4, file3, file2, file1] as File[]

        when:
        def sorted1 = FilesConcatenation.sortByTimestamp(approach1)
        def sorted2 = FilesConcatenation.sortByTimestamp(approach2)

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
    }

    private File getFile(String s) {
        return new File(getClass().getResource(s).toURI())
    }

}
