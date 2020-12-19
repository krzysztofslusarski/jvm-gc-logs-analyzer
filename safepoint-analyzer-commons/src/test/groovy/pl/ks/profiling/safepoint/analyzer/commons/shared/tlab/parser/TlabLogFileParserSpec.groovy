package pl.ks.profiling.safepoint.analyzer.commons.shared.tlab.parser

import spock.lang.Specification


class TlabLogFileParserSpec extends Specification {
    String newGcLog = "gc,start"
    String Thread1Log1 = "[2020-09-30T15:16:11.010+0200][485804.308s][2956][trace][gc,tlab              ] TLAB: fill thread: 0x00007fadfa299800 [id: 3078] desired_size: 632KB slow allocs: 23  refill waste: 10120B alloc: 0.01384      113KB refills: 1 waste  0.0% gc: 0B slow: 0B fast: 0B"
    String Thread2Log1 = "[2020-09-30T15:16:11.010+0200][485804.308s][2956][trace][gc,tlab              ] TLAB: gc thread: 0x00007fae20097800 [id: 13908] desired_size: 3KB slow allocs: 0  refill waste: 56B alloc: 0.00008        1KB refills: 1 waste  0.0% gc: 0B slow: 0B fast: 0B"
    String SomeGarbage = "[2020-09-30T15:16:11.010+0200][485804.308s][2956][trace][gc,tlab              ] ThreadLocalAllocBuffer::compute_size(6) returns 1808"
    String Thread3Log1 = "[2020-09-30T15:16:11.010+0200][485804.308s][2956][trace][gc,tlab              ] TLAB: fill thread: 0x00007fae20094800 [id: 13904] desired_size: 14KB slow allocs: 101  refill waste: 224B alloc: 0.00031        3KB refills: 1 waste  0.0% gc: 0B slow: 0B fast: 0B"
    String Thread2Log2 = "[2020-09-30T15:16:11.010+0200][485804.308s][2956][trace][gc,tlab              ] TLAB: gc thread: 0x00007fae20097800 [id: 13908] desired_size: 3KB slow allocs: 5  refill waste: 56B alloc: 0.00008        1KB refills: 2 waste  8.0% gc: 0B slow: 592B fast: 0B"

    def "should not complete parsing until next garbage collection"() {
        given:
        List<String> lineWithoutNewGcLog = [Thread1Log1]

        when:
        List<List<ThreadTlabBeforeGC>> allTlabStats = parseLines(lineWithoutNewGcLog).getThreadTlabsBeforeGC()

        then:
        allTlabStats.isEmpty()
    }

    def "should complete parsing when new garbage collection is present"() {
        given:
        List<String> lineWithoutNewGcLog = [Thread1Log1, newGcLog]

        when:
        List<List<ThreadTlabBeforeGC>> allTlabStats = parseLines(lineWithoutNewGcLog).getThreadTlabsBeforeGC()

        then:
        !allTlabStats.isEmpty()
    }

    def "should parse fill and gc TLAB logs"() {
        given:
        List<String> lines = [Thread1Log1, Thread2Log1]

        when:
        List<List<ThreadTlabBeforeGC>> tlabsBeforeGc = parseLinesWithNewGc(lines).getThreadTlabsBeforeGC()

        then:
        !tlabsBeforeGc.isEmpty()
        tlabsBeforeGc.head().size() == 2
    }

    def "should parse all expected values"() {
        given:
        List<String> lines = [Thread1Log1]

        when:
        ThreadTlabBeforeGC tlabsBeforeGc = parseLinesWithNewGc(lines).getThreadTlabsBeforeGC().head().head()

        then:
        tlabsBeforeGc.tid == "0x00007fadfa299800"
        tlabsBeforeGc.nid == 3078
        tlabsBeforeGc.size == 632 // 632KB
        tlabsBeforeGc.slowAllocs == 23
    }

    def "should keep only last occurrence for thread"() {
        given:
        List<String> lines = [Thread2Log1, Thread2Log2]

        when:
        List<ThreadTlabBeforeGC> statsForThreads = parseLinesWithNewGc(lines).getThreadTlabsBeforeGC().head()

        then:
        statsForThreads.size() == 1
        ThreadTlabBeforeGC stats = statsForThreads.head()
        stats.tid == "0x00007fae20097800"
        stats.slowAllocs == 5
    }

    def "should sort results by number of slow allocations"() {
        given:
        List<String> lines = [Thread1Log1, Thread2Log1, SomeGarbage, Thread3Log1, Thread2Log2, newGcLog]

        when:
        List<ThreadTlabBeforeGC> stats = parseLinesWithNewGc(lines).getThreadTlabsBeforeGC().head()
        def first = stats.get(0)
        def second = stats.get(1)
        def third = stats.get(2)

        then:
        first.slowAllocs >= second.slowAllocs
        second.slowAllocs >= third.slowAllocs
    }

    def "should parse line with gc tlab summary"() {
        given:
        List<String>  lines = ["[2020-09-30T15:14:55.030+0200][485728.328s][2956][debug][gc,tlab              ] GC(16717) TLAB totals: thrds: 86  refills: 2701 max: 265 slow allocs: 1750 max 245 waste:  1.2% gc: 19634872B max: 2669656B slow: 5174232B max: 897504B fast: 0B max: 0B"]

        when:
        def summary = parseLines(lines).getTlabSummaries().head()

        then:
        summary.timeStamp == 485728.328G
        summary.threadCount == 86
        summary.refills == 2701
        summary.maxRefills == 265
        summary.slowAllocs == 1750
        summary.maxSlowAllocs == 245
        summary.wastePercent == 1.2
    }

    def parseLinesWithNewGc(List<String> lines) {
        def newLines = new ArrayList<String>(lines)
        newLines.add(newGcLog)
        return parseLines(newLines)
    }

    def parseLines(List<String> lines) {
        TlabLogFileParser parser = new TlabLogFileParser()
        for (logLine in lines) {
            parser.parseLine(logLine)
        }
        return parser.fetchData()
    }
}