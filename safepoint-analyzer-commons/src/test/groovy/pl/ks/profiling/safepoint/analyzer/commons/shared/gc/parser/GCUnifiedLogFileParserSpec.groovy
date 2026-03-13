package pl.ks.profiling.safepoint.analyzer.commons.shared.gc.parser

import spock.lang.Specification

import static pl.ks.profiling.safepoint.analyzer.commons.shared.gc.parser.GCLogCycleEntry.*

class GCUnifiedLogFileParserSpec extends Specification {

    def "should parse gc log"() {
        given:
        GCUnifiedLogFileParser parser = new GCUnifiedLogFileParser()
        def gcLogs = getClass().getResourceAsStream("/g1gc.log").text

        when:
        gcLogs.split("\n").each {
            parser.parseLine(it)
        }
        GCLogCycleEntry gcEntry = parser.fetchData().cycleEntries.head()

        then:
        gcEntry.timeStamp == 1778483.410G
        gcEntry.sequenceId == 597760
        gcEntry.phase == "Pause Young (Concurrent Start) (G1 Humongous Allocation)"
        gcEntry.aggregatedPhase == "Young collection - piggybacks"
        gcEntry.heapBeforeGCMb == 1338
        gcEntry.heapAfterGCMb == 1324
        gcEntry.heapSizeMb == 5120
        gcEntry.timeMs == 6.363
        phaseTimeMax(gcEntry, PRE_EVACUATE) == 0.4G
        subphaseTimeMax(gcEntry, PRE_PREPARE_TLABS) == 0.0G
        subphaseTimeMax(gcEntry, PRE_CHOOSE_COLLECTION_SET) == 0.0G
        subphaseTimeMax(gcEntry, PRE_HUMONGOUS_REGISTER) == 0.2G
        subphaseTimeMax(gcEntry, PRE_CLEAR_CLAIMED_MARKS) == 0.3G
        phaseTimeMax(gcEntry, EVACUATE) == 2.3G
        subphaseTimeMax(gcEntry, EVACUATE_EXT_ROOT_SCANNING) == 1.2G
        subphaseTimeMax(gcEntry, EVACUATE_UPDATE_RS) == 1.1G
        subphaseTimeMax(gcEntry, EVACUATE_SCAN_RS) == 0.2G
        subphaseTimeMax(gcEntry, EVACUATE_CODE_ROOT_SCANNING) == 0.0G
        subphaseTimeMax(gcEntry, EVACUATE_AOT_ROOT_SCANNING) == 0.0G
        subphaseTimeMax(gcEntry, EVACUATE_OBJECT_COPY) == 0.4G
        subphaseTimeMax(gcEntry, EVACUATE_TERMINATION) == 0.2G
        subphaseTimeMax(gcEntry, EVACUATE_GC_WORKER_OTHER) == 0.0G
        subphaseTimeMax(gcEntry, EVACUATE_GC_WORKER_TOTAL) == 2.2G
        phaseTimeMax(gcEntry, POST_EVACUATE) == 1.9G
        subphaseTimeMax(gcEntry, POST_CODE_ROOTS_FIXUP) == 0.0G
        subphaseTimeMax(gcEntry, POST_CLEAR_CARD_TABLE) == 0.4G
        subphaseTimeMax(gcEntry, POST_REFERENCE_PROCESSING) == 0.1G
        subphaseTimeMax(gcEntry, POST_WEAK_PROCESSING) == 0.1G
        subphaseTimeMax(gcEntry, POST_MERGE_PER_THREAD_STATE) == 0.0G
        subphaseTimeMax(gcEntry, POST_CODE_ROOTS_PURGE) == 0.0G
        subphaseTimeMax(gcEntry, POST_REDIRTY_CARDS) == 0.0G
        subphaseTimeMax(gcEntry, POST_DERIVED_POINTER_TABLE_UPDATE) == 0.0G
        subphaseTimeMax(gcEntry, POST_FREE_COLLECTION_SET) == 0.4G
        subphaseTimeMax(gcEntry, POST_HUMONGOUS_RECLAIM) == 0.9G
        subphaseTimeMax(gcEntry, POST_START_NEW_COLLECTION_SET) == 0.0G
        subphaseTimeMax(gcEntry, POST_RESIZE_TLABS) == 0.0G
        subphaseTimeMax(gcEntry, POST_EXPAND_HEAP) == 0.0G
        phaseTimeMax(gcEntry, PHASE_OTHER) == 1.0G
        gcEntry.regionsBeforeGC[REGIONS_EDEN] == 7
        gcEntry.regionsBeforeGC[REGIONS_SURVIVOR] == 1
        gcEntry.regionsBeforeGC[REGIONS_OLD] == 526
        gcEntry.regionsBeforeGC[REGIONS_HUMONGOUS] == 137
        gcEntry.regionsAfterGC[REGIONS_EDEN] == 0
        gcEntry.regionsAfterGC[REGIONS_SURVIVOR] == 1
        gcEntry.regionsAfterGC[REGIONS_OLD] == 526
        gcEntry.regionsAfterGC[REGIONS_HUMONGOUS] == 136
        gcEntry.regionsMax[REGIONS_EDEN] == 1473
        gcEntry.regionsMax[REGIONS_SURVIVOR] == 184
        gcEntry.regionsMax[REGIONS_OLD] == null
        gcEntry.regionsMax[REGIONS_HUMONGOUS] == null
        gcEntry.regionsSizeAfterGC == Collections.emptyMap()
        gcEntry.regionsWastedAfterGC == Collections.emptyMap()
        gcEntry.liveHumongousSizes.size() == 87
        gcEntry.deadHumongousSizes.size() == 1
        !gcEntry.genuineCollection
        gcEntry.bytesInAges[1] == 94368
        gcEntry.bytesInAges[2] == 688
        gcEntry.bytesInAges[3] == 2768
        gcEntry.bytesInAges[4] == 24752
        gcEntry.bytesInAges[5] == 53688
        gcEntry.bytesInAges[6] == 456
        gcEntry.bytesInAges[7] == 288
        gcEntry.bytesInAges[8] == 640
        gcEntry.bytesInAges[9] == 288
        gcEntry.bytesInAges[10] == 288
        gcEntry.bytesInAges[11] == 288
        gcEntry.bytesInAges[12] == 312
        gcEntry.bytesInAges[13] == 312
        gcEntry.bytesInAges[14] == 336
        gcEntry.bytesInAges[15] == 336
        gcEntry.maxAge == 15
        gcEntry.desiredSurvivorSize == 192937984
        gcEntry.newTenuringThreshold == 15
        gcEntry.maxTenuringThreshold == 15
        !gcEntry.wasToSpaceExhausted
    }

    def "should parse time when 'ms' separated by space"() {
        given:
        GCUnifiedLogFileParser parser = new GCUnifiedLogFileParser()
        def gcLogsWithTimeSeparatedBySpace = getClass().getResourceAsStream("/g1gc-time-separated-by-space.log").text

        when:
        gcLogsWithTimeSeparatedBySpace.split("\n").each {
            parser.parseLine(it)
        }
        GCLogCycleEntry gcEntry = parser.fetchData().cycleEntries.head()

        then:
        gcEntry.timeStamp == 1778483.410G
        gcEntry.sequenceId == 597760
        gcEntry.phase == "Pause Young (Concurrent Start) (G1 Humongous Allocation)"
        gcEntry.aggregatedPhase == "Young collection - piggybacks"
        gcEntry.heapBeforeGCMb == 1338
        gcEntry.heapAfterGCMb == 1324
        gcEntry.heapSizeMb == 5120
        gcEntry.timeMs == 6.363
        phaseTimeMax(gcEntry, PRE_EVACUATE) == 0.4G
        subphaseTimeMax(gcEntry, PRE_PREPARE_TLABS) == 0.0G
        subphaseTimeMax(gcEntry, PRE_CHOOSE_COLLECTION_SET) == 0.0G
        subphaseTimeMax(gcEntry, PRE_HUMONGOUS_REGISTER) == 0.2G
        subphaseTimeMax(gcEntry, PRE_CLEAR_CLAIMED_MARKS) == 0.3G
        phaseTimeMax(gcEntry, EVACUATE) == 2.3G
        phaseTimeMax(gcEntry, POST_EVACUATE) == 1.9G
        subphaseTimeMax(gcEntry, POST_CODE_ROOTS_FIXUP) == 0.0G
        subphaseTimeMax(gcEntry, POST_CLEAR_CARD_TABLE) == 0.4G
        subphaseTimeMax(gcEntry, POST_REFERENCE_PROCESSING) == 0.1G
        subphaseTimeMax(gcEntry, POST_WEAK_PROCESSING) == 0.1G
        subphaseTimeMax(gcEntry, POST_MERGE_PER_THREAD_STATE) == 0.0G
        subphaseTimeMax(gcEntry, POST_CODE_ROOTS_PURGE) == 0.0G
        subphaseTimeMax(gcEntry, POST_REDIRTY_CARDS) == 0.0G
        subphaseTimeMax(gcEntry, POST_DERIVED_POINTER_TABLE_UPDATE) == 0.0G
        subphaseTimeMax(gcEntry, POST_FREE_COLLECTION_SET) == 0.4G
        subphaseTimeMax(gcEntry, POST_HUMONGOUS_RECLAIM) == 0.9G
        subphaseTimeMax(gcEntry, POST_START_NEW_COLLECTION_SET) == 0.0G
        subphaseTimeMax(gcEntry, POST_RESIZE_TLABS) == 0.0G
        subphaseTimeMax(gcEntry, POST_EXPAND_HEAP) == 0.0G
        phaseTimeMax(gcEntry, PHASE_OTHER) == 1.0G
        gcEntry.regionsBeforeGC[REGIONS_EDEN] == 7
        gcEntry.regionsBeforeGC[REGIONS_SURVIVOR] == 1
        gcEntry.regionsBeforeGC[REGIONS_OLD] == 526
        gcEntry.regionsBeforeGC[REGIONS_HUMONGOUS] == 137
        gcEntry.regionsAfterGC[REGIONS_EDEN] == 0
        gcEntry.regionsAfterGC[REGIONS_SURVIVOR] == 1
        gcEntry.regionsAfterGC[REGIONS_OLD] == 526
        gcEntry.regionsAfterGC[REGIONS_HUMONGOUS] == 136
        gcEntry.regionsMax[REGIONS_EDEN] == 1473
        gcEntry.regionsMax[REGIONS_SURVIVOR] == 184
        gcEntry.regionsMax[REGIONS_OLD] == null
        gcEntry.regionsMax[REGIONS_HUMONGOUS] == null
        gcEntry.regionsSizeAfterGC == Collections.emptyMap()
        gcEntry.regionsWastedAfterGC == Collections.emptyMap()
        !gcEntry.genuineCollection
        !gcEntry.wasToSpaceExhausted
    }

    def "should parse g1gc log with string dedup"() {
        given:
        GCUnifiedLogFileParser parser = new GCUnifiedLogFileParser()
        def logLines = getClass().getResourceAsStream("/g1gc-with-dedup.log").text

        when:
        logLines.split("\n").each {
            parser.parseLine(it)
        }
        GCLogFile gcLogFile = parser.fetchData()
        GCLogCycleEntry gcEntry = gcLogFile.cycleEntries.head()

        then:
        gcLogFile.cycleEntries.size() == 1
        gcLogFile.concurrentCycleEntries.size() == 0
        gcEntry.timeStamp == 1604566078.620G
        gcEntry.sequenceId == 21536
        gcEntry.phase == "Pause Young (Normal) (G1 Evacuation Pause)"
        gcEntry.aggregatedPhase == "Young collection"
        gcEntry.cause == "G1 Evacuation Pause"
        gcEntry.heapBeforeGCMb == 5855
        gcEntry.heapAfterGCMb == 5383
        gcEntry.heapSizeMb == 12288
        gcEntry.timeMs == 198.221
        gcEntry.genuineCollection
        !gcEntry.wasToSpaceExhausted
        phaseTimeMax(gcEntry, PRE_EVACUATE) == 0.1G
        subphaseTimeMax(gcEntry, PRE_PREPARE_TLABS) == 0.0G
        subphaseTimeMax(gcEntry, PRE_CHOOSE_COLLECTION_SET) == 0.0G
        subphaseTimeMax(gcEntry, PRE_HUMONGOUS_REGISTER) == 0.1G
        phaseTimeMax(gcEntry, EVACUATE) == 16.3G
        subphaseTimeMax(gcEntry, EVACUATE_EXT_ROOT_SCANNING) == 0.2G
        subphaseTimeMax(gcEntry, EVACUATE_UPDATE_RS) == 0.3G
        subphaseTimeMax(gcEntry, EVACUATE_SCAN_RS) == 0.1G
        subphaseTimeMax(gcEntry, EVACUATE_CODE_ROOT_SCANNING) == 0.0G
        subphaseTimeMax(gcEntry, EVACUATE_AOT_ROOT_SCANNING) == 0.0G
        subphaseTimeMax(gcEntry, EVACUATE_OBJECT_COPY) == 16.0G
        subphaseTimeMax(gcEntry, EVACUATE_TERMINATION) == 0.0G
        subphaseTimeMax(gcEntry, EVACUATE_GC_WORKER_OTHER) == 0.1G
        subphaseTimeMax(gcEntry, EVACUATE_GC_WORKER_TOTAL) == 16.3G
        phaseTimeMax(gcEntry, POST_EVACUATE) == 181.0G
        subphaseTimeMax(gcEntry, POST_CODE_ROOTS_FIXUP) == 0.0G
        subphaseTimeMax(gcEntry, POST_CLEAR_CARD_TABLE) == 0.2G
        subphaseTimeMax(gcEntry, POST_REFERENCE_PROCESSING) == 0.1G
        subphaseTimeMax(gcEntry, POST_WEAK_PROCESSING) == 0.1G
        subphaseTimeMax(gcEntry, STRING_DEDUP_FIXUP) == 180.2G
        subphaseTimeMax(gcEntry, POST_MERGE_PER_THREAD_STATE) == 0.0G
        subphaseTimeMax(gcEntry, POST_CODE_ROOTS_PURGE) == 0.0G
        subphaseTimeMax(gcEntry, POST_REDIRTY_CARDS) == 0.0G
        subphaseTimeMax(gcEntry, POST_DERIVED_POINTER_TABLE_UPDATE) == 0.0G
        subphaseTimeMax(gcEntry, POST_FREE_COLLECTION_SET) == 0.4G
        subphaseTimeMax(gcEntry, POST_HUMONGOUS_RECLAIM) == 0.0G
        subphaseTimeMax(gcEntry, POST_START_NEW_COLLECTION_SET) == 0.0G
        subphaseTimeMax(gcEntry, POST_RESIZE_TLABS) == 0.0G
        subphaseTimeMax(gcEntry, POST_EXPAND_HEAP) == 0.0G
        phaseTimeMax(gcEntry, PHASE_OTHER) == 0.3G
        gcEntry.regionsBeforeGC[REGIONS_EDEN] == 133
        gcEntry.regionsBeforeGC[REGIONS_SURVIVOR] == 20
        gcEntry.regionsBeforeGC[REGIONS_OLD] == 1156
        gcEntry.regionsBeforeGC[REGIONS_HUMONGOUS] == 155
        gcEntry.regionsAfterGC[REGIONS_EDEN] == 0
        gcEntry.regionsAfterGC[REGIONS_SURVIVOR] == 20
        gcEntry.regionsAfterGC[REGIONS_OLD] == 1171
        gcEntry.regionsAfterGC[REGIONS_HUMONGOUS] == 155
        gcEntry.regionsMax[REGIONS_EDEN] == 133
        gcEntry.regionsMax[REGIONS_SURVIVOR] == 20
        gcEntry.regionsMax[REGIONS_OLD] == null
        gcEntry.regionsMax[REGIONS_HUMONGOUS] == null
        gcEntry.regionsSizeAfterGC == Collections.emptyMap()
        gcEntry.regionsWastedAfterGC == Collections.emptyMap()
        gcEntry.liveHumongousSizes.size() == 0
        gcEntry.deadHumongousSizes.size() == 0
        gcEntry.bytesInAges.size() == 0
        gcEntry.maxAge == 0
        gcEntry.desiredSurvivorSize == 0
        gcEntry.newTenuringThreshold == 0
        gcEntry.maxTenuringThreshold == 0
    }

    BigDecimal phaseTimeMax(GCLogCycleEntry gcEntry, String phase) {
        return gcEntry.subPhasesTime[phase]
    }

    BigDecimal subphaseTimeMax(GCLogCycleEntry gcEntry, String subphase) {
        return gcEntry.subPhasesTime["|______${subphase}" as String]
    }

}
