package pl.ks.profiling.safepoint.analyzer.commons.shared.stringdedup.parser;

import java.math.BigDecimal;

class UnitsConverter {
    public static final BigDecimal MB = new BigDecimal(1024);
    public static final BigDecimal GB = MB.multiply(MB);

    public static BigDecimal megabytesToKiloBytes(BigDecimal mb) {
        return mb.multiply(MB);
    }

    public static BigDecimal gigabytesToKiloBytes(BigDecimal mb) {
        return mb.multiply(GB);
    }
}