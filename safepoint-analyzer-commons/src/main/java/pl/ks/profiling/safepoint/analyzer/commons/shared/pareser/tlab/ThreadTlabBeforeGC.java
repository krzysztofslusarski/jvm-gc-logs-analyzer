package pl.ks.profiling.safepoint.analyzer.commons.shared.pareser.tlab;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
class ThreadTlabBeforeGC {
    String tid;
    long nid;
    long size;
    long slowAllocs;
}
