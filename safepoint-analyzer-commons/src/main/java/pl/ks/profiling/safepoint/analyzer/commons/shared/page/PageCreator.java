package pl.ks.profiling.safepoint.analyzer.commons.shared.page;

import java.text.DecimalFormat;
import pl.ks.profiling.gui.commons.Page;
import pl.ks.profiling.safepoint.analyzer.commons.shared.pareser.safepoint.JvmLogFile;

public interface PageCreator {
    Page create(JvmLogFile jvmLogFile, DecimalFormat decimalFormat);
}
