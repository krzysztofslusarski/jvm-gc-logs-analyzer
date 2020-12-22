package pl.ks.profiling.safepoint.analyzer.commons.shared

import spock.lang.Specification

class ParserUtilsTest extends Specification {
    def "should get timestamp from line"() {
        given:
        String line = "[2020-12-21T01:04:47.091+0000][1778483.410s][info ][gc,start             ] GC(597760) Pause Young (Concurrent Start) (G1 Humongous Allocation)"

        when:
        BigDecimal timeStamp = ParserUtils.getTimeStamp(line)

        then:
        timeStamp == 1778483.410G
    }

    def "should get timestamp from line printed with other locale"() {
        given:
        String lineWithCommaInsteadOfDot = "[2020-12-21T01:04:47.091+0000][1778483,430s][info ][gc,start             ] GC(597760) Pause Young (Concurrent Start) (G1 Humongous Allocation)"

        when:
        BigDecimal timeStamp = ParserUtils.getTimeStamp(lineWithCommaInsteadOfDot)

        then:
        timeStamp == 1778483.430G
    }
}
