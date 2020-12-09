package pl.ks.profiling.safepoint.analyzer.commons.shared.stringdedup.parser;

import java.math.BigDecimal;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;

@Getter
public class StringDedupLogEntry {
    @Setter(AccessLevel.PACKAGE)
    private BigDecimal timeStamp;
    private boolean initialized;
    private long lastCount;
    private long lastCountNew;
    private long lastCountYoung;
    private long lastCountOld;
    private long totalCountNew;
    private long totalCount;
    private long totalCountYoung;
    private long totalCountOld;
    private BigDecimal lastSize;
    private BigDecimal lastSizeNew;
    private BigDecimal lastSizeYoung;
    private BigDecimal lastSizeOld;
    private BigDecimal totalSize;
    private BigDecimal totalSizeNew;
    private BigDecimal totalSizeYoung;
    private BigDecimal totalSizeOld;

    public void deduplicatedLast(long count, BigDecimal size) {
        lastCount = count;
        lastSize = size;
    }

    public void deduplicatedTotal(long count, BigDecimal size) {
        totalCount = count;
        totalSize = size;
    }

    public void youngLast(long count, BigDecimal size) {
        lastCountYoung = count;
        lastSizeYoung = size;
    }

    public void youngTotal(long count, BigDecimal size) {
        totalCountYoung = count;
        totalSizeYoung = size;
    }

    public void newLast(long count, BigDecimal size) {
        lastCountNew = count;
        lastSizeNew = size;
    }

    public void newTotal(long count, BigDecimal size) {
        totalCountNew = count;
        totalSizeNew = size;
    }

    public void oldLast(long count, BigDecimal size) {
        lastCountOld = count;
        lastSizeOld = size;
    }

    public void oldTotal(long count, BigDecimal size) {
        totalCountOld = count;
        totalSizeOld = size;
        initialized = true;
    }
}
