package pl.ks.profiling.safepoint.analyzer.commons.shared;

import java.text.DecimalFormat;
import pl.ks.profiling.gui.commons.Page;

public interface PageCreator {
    Page create(JvmLogFile jvmLogFile, DecimalFormat decimalFormat);
}
