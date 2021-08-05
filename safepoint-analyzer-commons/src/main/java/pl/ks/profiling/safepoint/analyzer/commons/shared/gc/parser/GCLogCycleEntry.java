/*
 * Copyright 2020 Krzysztof Slusarski, Artur Owczarek
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
package pl.ks.profiling.safepoint.analyzer.commons.shared.gc.parser;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.Getter;

@Getter
public class GCLogCycleEntry {
    public static final String MIXED_COLLECTION = "Mixed collection";
    public static final String FULL_COLLECTION = "Full collection";
    public static final String YOUNG_COLLECTION = "Young collection";

    public static final String REMARK_COLLECTION = "remark";

    private long sequenceId;
    private BigDecimal timeStamp;
    private String phase;
    private String aggregatedPhase;
    private String cause;

    private int heapBeforeGCMb;
    private int heapAfterGCMb;
    private int heapSizeMb;
    private BigDecimal timeMs;

    public String toString() {
        return sequenceId + " " + timeStamp + " " + heapBeforeGCMb + " " + heapAfterGCMb + " " + heapSizeMb;
    }

    private Map<String, BigDecimal> subPhasesTime = new LinkedHashMap<>();

    private Map<String, Integer> regionsAfterGC = new HashMap<>();
    private Map<String, Integer> regionsBeforeGC = new HashMap<>();
    private Map<String, Integer> regionsMax = new HashMap<>();

    private Map<String, Integer> regionsSizeAfterGC = new HashMap<>();
    private Map<String, Integer> regionsWastedAfterGC = new HashMap<>();

    private List<Long> liveHumongousSizes = new ArrayList<>();
    private List<Long> deadHumongousSizes = new ArrayList<>();

    boolean genuineCollection;

    private Map<Integer, Long> bytesInAges = new HashMap<>();
    private int maxAge;

    private long desiredSurvivorSize;
    private long newTenuringThreshold;
    private long maxTenuringThreshold;

    private boolean wasToSpaceExhausted;

    public static final String PRE_EVACUATE = "Pre Evacuate Collection Set";
    public static final String PRE_PREPARE_TLABS = "Prepare TLABs";
    public static final String PRE_CHOOSE_COLLECTION_SET = "Choose Collection Set";
    public static final String PRE_HUMONGOUS_REGISTER = "Humongous Register";
    public static final String PRE_CLEAR_CLAIMED_MARKS = "Clear Claimed Marks";
    public static final String EVACUATE = "Evacuate Collection Set";
    public static final String EVACUATE_EXT_ROOT_SCANNING = "Ext Root Scanning";
    public static final String EVACUATE_UPDATE_RS = "Update RS";
    public static final String EVACUATE_SCAN_RS = "Scan RS";
    public static final String EVACUATE_CODE_ROOT_SCANNING = "Code Root Scanning";
    public static final String EVACUATE_AOT_ROOT_SCANNING = "AOT Root Scanning";
    public static final String EVACUATE_OBJECT_COPY = "Object Copy";
    public static final String EVACUATE_TERMINATION = "Termination";
    public static final String EVACUATE_GC_WORKER_OTHER = "GC Worker Other";
    public static final String EVACUATE_GC_WORKER_TOTAL = "GC Worker Total";
    public static final String POST_EVACUATE = "Post Evacuate Collection Set";
    public static final String POST_CODE_ROOTS_FIXUP = "Code Roots Fixup";
    public static final String POST_CLEAR_CARD_TABLE = "Clear Card Table";
    public static final String POST_REFERENCE_PROCESSING = "Reference Processing";
    public static final String POST_WEAK_PROCESSING = "Weak Processing";
    public static final String POST_MERGE_PER_THREAD_STATE = "Merge Per-Thread State";
    public static final String POST_CODE_ROOTS_PURGE = "Code Roots Purge";
    public static final String POST_REDIRTY_CARDS = "Redirty Cards";
    public static final String POST_DERIVED_POINTER_TABLE_UPDATE = "DerivedPointerTable Update";
    public static final String POST_FREE_COLLECTION_SET = "Free Collection Set";
    public static final String POST_HUMONGOUS_RECLAIM = "Humongous Reclaim";
    public static final String POST_START_NEW_COLLECTION_SET = "Start New Collection Set";
    public static final String POST_RESIZE_TLABS = "Resize TLABs";
    public static final String POST_EXPAND_HEAP = "Expand Heap After Collection";
    public static final String PHASE_OTHER = "Other";

    public static final String REGIONS_EDEN = "Eden regions";
    public static final String REGIONS_SURVIVOR = "Survivor regions";
    public static final String REGIONS_OLD = "Old regions";
    public static final String REGIONS_HUMONGOUS = "Humongous regions";

    GCLogCycleEntry(Long sequenceId, String phase, BigDecimal timeStamp) {
        this.sequenceId = sequenceId;
        this.phase = phase;
        this.timeStamp = timeStamp;

        if (phase.contains("Pause Young")) {
            if (phase.contains("(Mixed)")) {
                aggregatedPhase = MIXED_COLLECTION;
                genuineCollection = true;
            } else if (phase.contains("(Allocation Failure)")) {
                aggregatedPhase = YOUNG_COLLECTION;
                genuineCollection = true;
            } else if (phase.contains("(Normal)")) {
                aggregatedPhase = YOUNG_COLLECTION;
                genuineCollection = true;
            } else {
                aggregatedPhase = "Young collection - piggybacks";
            }
        } else if (phase.contains("Pause Full")) {
            aggregatedPhase = FULL_COLLECTION;
            genuineCollection = true;
        } else if (phase.startsWith("(G1") || phase.startsWith("(GC") || phase.startsWith("(Meta")) {
            if (phase.contains("(mixed)")) {
                aggregatedPhase = MIXED_COLLECTION;
                genuineCollection = true;
            } else if (phase.endsWith("(young)") && !phase.contains("G1 Humongous Allocation")) {
                aggregatedPhase = YOUNG_COLLECTION;
                genuineCollection = true;
            } else {
                aggregatedPhase = "Young collection - piggybacks";
            }
        } else if (phase.contains("Full")) {
            aggregatedPhase = FULL_COLLECTION;
            genuineCollection = true;
        } else {
            aggregatedPhase = phase;
        }

        fillCause();
    }

    private void fillCause() {
        int start = phase.lastIndexOf('(');
        int end = phase.lastIndexOf(')');
        if (start >= 0 && end >= 0) {
            cause = phase.substring(start + 1, end);
        }
    }

    void addSubPhaseTime(String phase, BigDecimal time) {
        subPhasesTime.put(phase, time);
    }

    void addSizesAndTime(int heapBeforeGC, int heapAfterGC, int heapSize, BigDecimal time) {
        this.heapBeforeGCMb = heapBeforeGC;
        this.heapAfterGCMb = heapAfterGC;
        this.heapSizeMb = heapSize;
        this.timeMs = time;
    }

    void addHeapBeforeAndAfterGC(int heapBeforeGCMb, int heapAfterGCMb) {
        this.heapBeforeGCMb = heapBeforeGCMb;
        this.heapAfterGCMb = heapAfterGCMb;
    }

    void addHeapCapacityAfterGC(int capacity) {
        this.heapSizeMb = capacity;
    }

    void sumUpSubPhaseTimes() {
        BigDecimal sum = this.subPhasesTime.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        System.out.println("Total time for " + sequenceId + ":" + sum + "ms");
        this.timeMs = sum;
    }

    void addSizes(int heapBeforeGCMb, int heapAfterGCMb, int heapSizeMb) {
        this.heapBeforeGCMb = heapBeforeGCMb;
        this.heapAfterGCMb = heapAfterGCMb;
        this.heapSizeMb = heapSizeMb;
    }

    void addRegionCount(String regionName, Integer beforeGC, Integer afterGC, Integer maxRegions) {
        regionsBeforeGC.put(regionName, beforeGC);
        regionsAfterGC.put(regionName, afterGC);
        regionsMax.put(regionName, maxRegions);
    }

    void addRegionSizes(String regionName, Integer size, Integer wasted) {
        regionsSizeAfterGC.put(regionName, size);
        regionsWastedAfterGC.put(regionName, wasted);
    }

    void addLiveHumongous(Long size) {
        liveHumongousSizes.add(size);
    }

    void addDeadHumongous(Long size) {
        deadHumongousSizes.add(size);
    }

    void addAgeWithSize(int age, long size) {
        bytesInAges.put(age, size);
        maxAge = Math.max(age, maxAge);
    }

    void toSpaceExhausted() {
        wasToSpaceExhausted = true;
    }

    void addSurvivorStats(long desiredSize, long newThreshold, long maxThreshold) {
        this.desiredSurvivorSize = desiredSize;
        this.newTenuringThreshold = newThreshold;
        this.maxTenuringThreshold = maxThreshold;
    }

    void addTime(BigDecimal phaseTime) {
        this.timeMs = phaseTime;
    }

    boolean isMixed() {
        return MIXED_COLLECTION.equals(aggregatedPhase);
    }

    boolean isRemark() {
        return phase.toLowerCase().contains(REMARK_COLLECTION);
    }
}
