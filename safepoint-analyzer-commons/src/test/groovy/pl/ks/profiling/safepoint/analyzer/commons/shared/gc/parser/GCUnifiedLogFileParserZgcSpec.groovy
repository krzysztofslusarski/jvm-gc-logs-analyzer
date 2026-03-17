package pl.ks.profiling.safepoint.analyzer.commons.shared.gc.parser

import spock.lang.Specification

import java.util.zip.GZIPInputStream

import static pl.ks.profiling.safepoint.analyzer.commons.shared.gc.parser.GCLogCycleEntry.*

class GCUnifiedLogFileParserZgcSpec extends Specification {

    private static String readGzippedResource(String path) {
        def stream = GCUnifiedLogFileParserZgcSpec.class.getResourceAsStream(path)
        return new GZIPInputStream(stream).text
    }

    def "should parse zgc log"() {
        given:
        GCUnifiedLogFileParser parser = new GCUnifiedLogFileParser(GCCollectorType.ZGC)
        def gcLogs = readGzippedResource("/zgc.log.gz")

        when:
        gcLogs.split("\n").each {
            parser.parseLine(it)
        }
        GCLogFile gcLogFile = parser.fetchData()
        GCLogCycleEntry firstCycle = gcLogFile.cycleEntries.first()

        then:
        gcLogFile.cycleEntries.size() == 145
        firstCycle.sequenceId == 0
        firstCycle.phase == "Major Collection (Metadata GC Threshold)"
        firstCycle.aggregatedPhase == ZGC_MAJOR_CYCLE
        firstCycle.genuineCollection
        firstCycle.heapBeforeGCMb == 140
        firstCycle.heapAfterGCMb == 80
        firstCycle.heapSizeMb == -1
        firstCycle.timeMs > BigDecimal.ZERO
        !firstCycle.wasToSpaceExhausted
    }

    def "should have correct sub-phase times for first zgc major cycle"() {
        given:
        GCUnifiedLogFileParser parser = new GCUnifiedLogFileParser(GCCollectorType.ZGC)
        def gcLogs = readGzippedResource("/zgc.log.gz")

        when:
        gcLogs.split("\n").each {
            parser.parseLine(it)
        }
        GCLogFile gcLogFile = parser.fetchData()
        def subPhases = gcLogFile.cycleEntries.first().subPhasesTime

        then: "young generation phases"
        subPhases["Y: Pause Mark Start (Major)"] == 0.012G
        subPhases["Y: Concurrent Mark"] == 39.561G
        subPhases["Y: Pause Mark End"] == 0.011G
        subPhases["Y: Concurrent Mark Free"] == 0.001G
        subPhases["Y: Concurrent Reset Relocation Set"] == 0.000G
        subPhases["Y: Concurrent Select Relocation Set"] == 1.676G
        subPhases["Y: Pause Relocate Start"] == 0.009G
        subPhases["Y: Concurrent Relocate"] == 19.705G

        and: "old generation phases"
        subPhases["O: Concurrent Mark"] == 0.564G
        subPhases["O: Pause Mark End"] == 0.018G
        subPhases["O: Concurrent Mark Free"] == 0.000G
        subPhases["O: Concurrent Process Non-Strong"] == 7.543G
        subPhases["O: Concurrent Reset Relocation Set"] == 0.000G
        subPhases["O: Concurrent Select Relocation Set"] == 0.297G
        subPhases["O: Concurrent Remap Roots"] == 1.483G
        subPhases["O: Pause Relocate Start"] == 0.011G
        subPhases["O: Concurrent Relocate"] == 0.022G
    }

    def "should parse minor collection cycle"() {
        given:
        GCUnifiedLogFileParser parser = new GCUnifiedLogFileParser(GCCollectorType.ZGC)
        def gcLogs = readGzippedResource("/zgc.log.gz")

        when:
        gcLogs.split("\n").each {
            parser.parseLine(it)
        }
        GCLogFile gcLogFile = parser.fetchData()
        GCLogCycleEntry minorCycle = gcLogFile.cycleEntries.find { it.sequenceId == 6 }

        then:
        minorCycle.phase == "Minor Collection (Allocation Rate)"
        minorCycle.aggregatedPhase == ZGC_MINOR_CYCLE
        minorCycle.genuineCollection
        minorCycle.heapBeforeGCMb == 2126
        minorCycle.heapAfterGCMb == 888
        minorCycle.heapSizeMb == -1
        minorCycle.subPhasesTime.size() > 0
    }

    def "should parse all zgc cycles correctly"() {
        given:
        GCUnifiedLogFileParser parser = new GCUnifiedLogFileParser(GCCollectorType.ZGC)
        def gcLogs = readGzippedResource("/zgc.log.gz")

        when:
        gcLogs.split("\n").each {
            parser.parseLine(it)
        }
        GCLogFile gcLogFile = parser.fetchData()

        then:
        gcLogFile.cycleEntries.every {
            it.aggregatedPhase == ZGC_MAJOR_CYCLE || it.aggregatedPhase == ZGC_MINOR_CYCLE
        }
        gcLogFile.cycleEntries.every { it.genuineCollection }
        gcLogFile.cycleEntries.every { it.timeMs != null && it.timeMs > BigDecimal.ZERO }
        gcLogFile.cycleEntries.every { it.subPhasesTime.size() > 0 }
    }
}
