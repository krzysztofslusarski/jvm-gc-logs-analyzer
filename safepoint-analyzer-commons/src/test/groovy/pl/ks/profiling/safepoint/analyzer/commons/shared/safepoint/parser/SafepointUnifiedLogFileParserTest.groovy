package pl.ks.profiling.safepoint.analyzer.commons.shared.safepoint.parser

import spock.lang.Specification

class SafepointUnifiedLogFileParserTest extends Specification {
    def "should parse jdk 19 log properly"() {
        given:
        SafepointUnifiedLogFileParser parser = new SafepointUnifiedLogFileParser()

        when:
        parser.parseLine("[2023-07-17T18:34:54.989+0200][0.087s][info ][safepoint      ] Safepoint \"ChangeBreakpoints\", Time since last: 18420792 ns, Reaching safepoint: 21958 ns, At safepoint: 85292 ns, Total: 107250 ns")

        then:
        SafepointLogEntry entry = parser.fetchData().safepoints.head()

        entry.timeStamp == 0.087
        entry.sequenceId == 0
        entry.operationName == "ChangeBreakpoints"
        entry.applicationTime == 0.018420792G
        entry.ttsTime == 0.000021958G
        entry.stoppedTime == 0.000107250
        entry.completed
    }

    def "should should parse graal log properly"() {
        given:
        SafepointUnifiedLogFileParser parser = new SafepointUnifiedLogFileParser()

        when:
        parser.parseLine("[2023-07-17T08:20:21.129+0000][5086.354s][info ][safepoint      ] Safepoint \"G1CollectForAllocation\", Time since last: 5972932511 ns, Reaching safepoint: 108298 ns, Cleanup: 83917 ns, At safepoint: 113194973 ns, Total: 113387188 ns")

        then:
        SafepointLogEntry entry = parser.fetchData().safepoints.head()
        entry.timeStamp == 5086.354G
        entry.sequenceId == 0
        entry.operationName == "G1CollectForAllocation"
        entry.applicationTime == 5.972932511G
        entry.ttsTime == 0.000108298G
        entry.stoppedTime == 0.113387188G
        entry.completed
    }
}
