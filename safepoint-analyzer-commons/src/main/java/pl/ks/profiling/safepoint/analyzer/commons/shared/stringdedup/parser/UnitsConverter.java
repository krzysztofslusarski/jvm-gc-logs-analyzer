package pl.ks.profiling.safepoint.analyzer.commons.shared.stringdedup.parser;

import java.math.BigDecimal;
import java.math.RoundingMode;

class UnitsConverter {
    public static final BigDecimal KB_TO_MB_MULTIPLIER = new BigDecimal(1024);
    public static final BigDecimal KB_TO_GB_MULTIPLIER = KB_TO_MB_MULTIPLIER.multiply(KB_TO_MB_MULTIPLIER);
    public static final BigDecimal B_TO_KB_DIVISOR = new BigDecimal(1024);

    public static BigDecimal bytesToKiloBytes(BigDecimal bytes) {
        return bytes.divide(B_TO_KB_DIVISOR, 2, RoundingMode.HALF_EVEN);
    }

    public static BigDecimal megabytesToKiloBytes(BigDecimal megaBytes) {
        return megaBytes.multiply(KB_TO_MB_MULTIPLIER);
    }

    public static BigDecimal gigabytesToKiloBytes(BigDecimal gigaBytes) {
        return gigaBytes.multiply(KB_TO_GB_MULTIPLIER);
    }
}