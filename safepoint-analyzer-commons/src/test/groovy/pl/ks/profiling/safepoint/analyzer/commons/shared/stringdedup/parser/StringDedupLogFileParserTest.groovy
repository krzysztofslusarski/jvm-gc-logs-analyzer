package pl.ks.profiling.safepoint.analyzer.commons.shared.stringdedup.parser

import spock.lang.Specification

class StringDedupLogFileParserTest extends Specification {
    def "should parse string deduplication stats"() {
        given:
        def stringDedupLogFileParser = new StringDedupLogFileParser()

        def fileLines = '''
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
[2020-11-26T14:39:30.003+0000][2689.530s][info ][gc,stringdedup       ] Concurrent String Deduplication (2689.530s)
'''.split( '\n' )

        when:
        for (logLine in fileLines) {
            stringDedupLogFileParser.parseLine(logLine)
        }
        def logFile = stringDedupLogFileParser.fetchData().entries.head()

        then:
        logFile.timeStamp == 2689.530
        logFile.initialized
        logFile.lastCountNew == 188
        logFile.lastCount == 174
        logFile.lastCountYoung == 0
        logFile.lastCountOld == 174
        logFile.totalCountNew == 23858866
        logFile.totalCount == 5926557
        logFile.totalCountYoung == 31584
        logFile.totalCountOld == 5894973
        logFile.lastSize == 5.81 // 5952.0B
        logFile.lastSizeNew == 6.77 // 6936.0B
        logFile.lastSizeYoung == 0.0
        logFile.lastSizeOld ==  5.81  // 5952.0B
        logFile.totalSize == 239820.8 // 234.2M
        logFile.totalSizeNew == 1017446.4 // 993.6M
        logFile.totalSizeYoung == 1504.2 // 1504.2K
        logFile.totalSizeOld == 238284.8 // 232.7M
    }
}
