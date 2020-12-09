package pl.ks.profiling.safepoint.analyzer.commons.shared.tlab.parser;

import java.math.BigDecimal;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class TlabSummaryInfo {
    BigDecimal timeStamp;
    long threadCount;
    long refills;
    long maxRefills;
    long slowAllocs;
    long maxSlowAllocs;
    BigDecimal wastePercent;
}
