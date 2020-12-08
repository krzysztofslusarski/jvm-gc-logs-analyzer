package pl.ks.profiling.gui.commons;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;

public class PageCreatorHelper {
    public static String numToString(BigDecimal bigDecimal, DecimalFormat decimalFormat) {
        if (bigDecimal == null) {
            return null;
        }
        return decimalFormat.format(bigDecimal.setScale(2, RoundingMode.HALF_EVEN)).replaceAll(",", " ");
    }

    public static String numToString(Double number, DecimalFormat decimalFormat) {
        if (number == null) {
            return null;
        }
        return decimalFormat.format(number).replaceAll(",", " ");
    }
}
