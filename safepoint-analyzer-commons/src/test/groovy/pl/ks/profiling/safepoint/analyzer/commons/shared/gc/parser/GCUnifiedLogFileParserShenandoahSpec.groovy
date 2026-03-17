package pl.ks.profiling.safepoint.analyzer.commons.shared.gc.parser

import spock.lang.Specification

import java.util.zip.GZIPInputStream

import static pl.ks.profiling.safepoint.analyzer.commons.shared.gc.parser.GCLogCycleEntry.*

class GCUnifiedLogFileParserShenandoahSpec extends Specification {

    private static String readGzippedResource(String path) {
        def stream = GCUnifiedLogFileParserShenandoahSpec.class.getResourceAsStream(path)
        return new GZIPInputStream(stream).text
    }

    def "should parse shenandoah gc log"() {
        given:
        GCUnifiedLogFileParser parser = new GCUnifiedLogFileParser(GCCollectorType.SHENANDOAH)
        def gcLogs = readGzippedResource("/shenandoah.log.gz")

        when:
        gcLogs.split("\n").each {
            parser.parseLine(it)
        }
        GCLogFile gcLogFile = parser.fetchData()
        GCLogCycleEntry firstCycle = gcLogFile.cycleEntries.first()

        then:
        gcLogFile.cycleEntries.size() == 31
        firstCycle.sequenceId == 0
        firstCycle.phase == "Concurrent reset (Global) (unload classes)"
        firstCycle.aggregatedPhase == SHENANDOAH_CYCLE
        firstCycle.genuineCollection
        firstCycle.heapBeforeGCMb == 127
        firstCycle.heapAfterGCMb == 49
        firstCycle.heapSizeMb == 2560
        firstCycle.timeMs > BigDecimal.ZERO
        !firstCycle.wasToSpaceExhausted
    }

    def "should have correct sub-phase times for first cycle"() {
        given:
        GCUnifiedLogFileParser parser = new GCUnifiedLogFileParser(GCCollectorType.SHENANDOAH)
        def gcLogs = readGzippedResource("/shenandoah.log.gz")

        when:
        gcLogs.split("\n").each {
            parser.parseLine(it)
        }
        GCLogFile gcLogFile = parser.fetchData()
        def subPhases = gcLogFile.cycleEntries.first().subPhasesTime

        then:
        subPhases["Concurrent reset (Global) (unload classes)"] == 5.183G
        subPhases["Pause Init Mark (Global) (unload classes)"] == 0.092G
        subPhases["Concurrent marking roots"] == 9.317G
        subPhases["Concurrent marking (Global) (unload classes)"] == 66.969G
        subPhases["Pause Final Mark (Global) (unload classes)"] == 0.564G
        subPhases["Concurrent thread roots"] == 10.602G
        subPhases["Concurrent weak references (Global) (unload classes)"] == 0.929G
        subPhases["Concurrent weak roots (Global) (unload classes)"] == 3.423G
        subPhases["Concurrent class unloading"] == 9.857G
        subPhases["Concurrent strong roots"] == 1.466G
        subPhases["Concurrent evacuation"] == 22.643G
        subPhases["Concurrent Init Update Refs (Global) (unload classes)"] == 0.201G
        subPhases["Pause Init Update Refs"] == 0.022G
        subPhases["Concurrent update references"] == 26.114G
        subPhases["Concurrent update thread roots"] == 0.357G
        subPhases["Pause Final Update Refs"] == 0.215G
        subPhases["Concurrent cleanup (Global) (unload classes)"] == 0.055G
        subPhases["Coalescing and filling old regions"] == 0.056G
        subPhases["Concurrent reset after collect (Global) (unload classes)"] == 0.882G
    }

    def "should parse all shenandoah gc cycles correctly"() {
        given:
        GCUnifiedLogFileParser parser = new GCUnifiedLogFileParser(GCCollectorType.SHENANDOAH)
        def gcLogs = readGzippedResource("/shenandoah.log.gz")

        when:
        gcLogs.split("\n").each {
            parser.parseLine(it)
        }
        GCLogFile gcLogFile = parser.fetchData()

        then:
        gcLogFile.cycleEntries.every { it.aggregatedPhase == SHENANDOAH_CYCLE }
        gcLogFile.cycleEntries.every { it.genuineCollection }
        gcLogFile.cycleEntries.every { it.timeMs != null && it.timeMs > BigDecimal.ZERO }
        gcLogFile.cycleEntries.every { it.subPhasesTime.size() > 0 }
        gcLogFile.cycleEntries.every { it.heapSizeMb == 2560 || it.heapSizeMb == 0 }
    }

    def "should parse age data for shenandoah"() {
        given:
        GCUnifiedLogFileParser parser = new GCUnifiedLogFileParser(GCCollectorType.SHENANDOAH)
        def gcLogs = readGzippedResource("/shenandoah.log.gz")

        when:
        gcLogs.split("\n").each {
            parser.parseLine(it)
        }
        GCLogFile gcLogFile = parser.fetchData()
        GCLogCycleEntry secondCycle = gcLogFile.cycleEntries.find { it.sequenceId == 1 }

        then:
        secondCycle.bytesInAges[1] == 38675848
        secondCycle.maxAge == 1
    }
}
