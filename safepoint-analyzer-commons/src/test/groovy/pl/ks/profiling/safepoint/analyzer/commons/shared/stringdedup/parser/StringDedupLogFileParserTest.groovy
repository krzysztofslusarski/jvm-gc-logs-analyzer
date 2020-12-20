/*
 * Copyright 2020 Artur Owczarek, Krzysztof Slusarski
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
package pl.ks.profiling.safepoint.analyzer.commons.shared.stringdedup.parser

import spock.lang.Specification

class StringDedupLogFileParserTest extends Specification {
    String stringDeduplicationLog = '''
[2020-11-26T14:39:28.003+0000][2689.530s][info ][gc,stringdedup       ] Concurrent String Deduplication (2689.530s)
[2020-11-26T14:39:28.003+0000][2689.530s][info ][gc,stringdedup       ] Concurrent String Deduplication 6936.0B->984.0B(5952.0B) avg 23.6% (2689.530s, 2689.530s) 0.171ms
[2020-11-26T14:39:28.004+0000][2689.530s][debug][gc,stringdedup       ]   Last Exec: 0.171ms, Idle: 579.950ms, Blocked: 0/0.000ms
[2020-11-26T14:39:28.004+0000][2689.530s][debug][gc,stringdedup       ]     Inspected:             188
[2020-11-26T14:39:28.004+0000][2689.530s][debug][gc,stringdedup       ]       Skipped:               0(  0.0%)
[2020-11-26T14:39:28.004+0000][2689.530s][debug][gc,stringdedup       ]       Hashed:               27( 14.4%)
[2020-11-26T14:39:28.004+0000][2689.530s][debug][gc,stringdedup       ]       Known:                 0(  0.0%)
[2020-11-26T14:39:28.004+0000][2689.530s][debug][gc,stringdedup       ]       New:                 188(100.0%)   6936.0B
[2020-11-26T14:39:28.004+0000][2689.530s][debug][gc,stringdedup       ]     Deduplicated:          174( 92.6%)   5952.0B( 85.8%)
[2020-11-26T14:39:28.004+0000][2689.530s][debug][gc,stringdedup       ]       Young:                 0(  0.0%)      0.0B(  0.0%)
[2020-11-26T14:39:28.004+0000][2689.530s][debug][gc,stringdedup       ]       Old:                 174(100.0%)   5952.0B(100.0%)
[2020-11-26T14:39:28.004+0000][2689.531s][debug][gc,stringdedup       ]   Total Exec: 2151/15000.151ms, Idle: 2151/2671082.764ms, Blocked: 63/2626.315ms
[2020-11-26T14:39:28.004+0000][2689.531s][debug][gc,stringdedup       ]     Inspected:        23916692
[2020-11-26T14:39:28.004+0000][2689.531s][debug][gc,stringdedup       ]       Skipped:               0(  0.0%)
[2020-11-26T14:39:28.004+0000][2689.531s][debug][gc,stringdedup       ]       Hashed:          3289556( 13.8%)
[2020-11-26T14:39:28.004+0000][2689.531s][debug][gc,stringdedup       ]       Known:             57826(  0.2%)
[2020-11-26T14:39:28.004+0000][2689.531s][debug][gc,stringdedup       ]       New:            23858866( 99.8%)    993.6M
[2020-11-26T14:39:28.004+0000][2689.531s][debug][gc,stringdedup       ]     Deduplicated:      5926557( 24.8%)    234.2M( 23.6%)
[2020-11-26T14:39:28.004+0000][2689.531s][debug][gc,stringdedup       ]       Young:             31584(  0.5%)   1504.2K(  0.6%)
[2020-11-26T14:39:28.004+0000][2689.531s][debug][gc,stringdedup       ]       Old:             5894973( 99.5%)    232.7M( 99.4%)
[2020-11-26T14:39:28.004+0000][2689.531s][debug][gc,stringdedup       ]   Table
[2020-11-26T14:39:28.004+0000][2689.531s][debug][gc,stringdedup       ]     Memory Usage: 536.1M
[2020-11-26T14:39:28.004+0000][2689.531s][debug][gc,stringdedup       ]     Size: 16777216, Min: 1024, Max: 16777216
[2020-11-26T14:39:28.004+0000][2689.531s][debug][gc,stringdedup       ]     Entries: 17811795, Load: 106.2%, Cached: 17559, Added: 17987127, Removed: 175332
[2020-11-26T14:39:28.004+0000][2689.531s][debug][gc,stringdedup       ]     Resize Count: 14, Shrink Threshold: 11184810(66.7%), Grow Threshold: 33554432(200.0%)
[2020-11-26T14:39:28.004+0000][2689.531s][debug][gc,stringdedup       ]     Rehash Count: 0, Rehash Threshold: 120, Hash Seed: 0x0
[2020-11-26T14:39:28.004+0000][2689.531s][debug][gc,stringdedup       ]     Age Threshold: 3
[2020-11-26T14:39:28.004+0000][2689.531s][debug][gc,stringdedup       ]   Queue
[2020-11-26T14:39:28.004+0000][2689.531s][debug][gc,stringdedup       ]     Dropped: 2453659
'''
    String beginningOfNextDeduplication = "[2020-11-26T14:39:30.003+0000][2689.530s][info ][gc,stringdedup       ] Concurrent String Deduplication (2689.530s)"

    String makeStringDeduplicationLogComplete(String log) {
        return log + "\n" + beginningOfNextDeduplication
    }


    def "should parse string deduplication stats"() {
        given:
        String deduplicationLogs = makeStringDeduplicationLogComplete(stringDeduplicationLog)

        when:
        StringDedupLogEntry parsedStats = parse(deduplicationLogs).head()

        then:
        parsedStats.timeStamp == 2689.530
        parsedStats.initialized
        parsedStats.lastCountNew == 188
        parsedStats.lastCount == 174
        parsedStats.lastCountYoung == 0
        parsedStats.lastCountOld == 174
        parsedStats.totalCountNew == 23858866
        parsedStats.totalCount == 5926557
        parsedStats.totalCountYoung == 31584
        parsedStats.totalCountOld == 5894973
        parsedStats.lastSize == 5.81 // 5952.0B
        parsedStats.lastSizeNew == 6.77 // 6936.0B
        parsedStats.lastSizeYoung == 0.0
        parsedStats.lastSizeOld == 5.81 // 5952.0B
        parsedStats.totalSize == 239820.8 // 234.2M
        parsedStats.totalSizeNew == 1017446.4 // 993.6M
        parsedStats.totalSizeYoung == 1504.2 // 1504.2K
        parsedStats.totalSizeOld == 238284.8 // 232.7M
    }

    def "should not complete parsing until meets next deduplication"() {
        when:
        List<StringDedupLogEntry> listOfStats = parse(stringDeduplicationLog)

        then:
        listOfStats.isEmpty()
    }

    def parse(String logs) {
        StringDedupLogFileParser parser = new StringDedupLogFileParser()
        String[] lines = logs.split('\n')
        for (logLine in lines) {
            parser.parseLine(logLine)
        }
        return parser.fetchData().entries
    }
}
