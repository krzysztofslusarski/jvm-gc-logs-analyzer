package pl.ks.profiling.safepoint.analyzer.commons.shared;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;
import pl.ks.profiling.gui.commons.Page;
import pl.ks.profiling.safepoint.analyzer.commons.shared.classloader.parser.ClassLoaderLogFile;
import pl.ks.profiling.safepoint.analyzer.commons.shared.gc.parser.GCLogFile;
import pl.ks.profiling.safepoint.analyzer.commons.shared.jit.parser.JitLogFile;
import pl.ks.profiling.safepoint.analyzer.commons.shared.safepoint.parser.SafepointLogFile;
import pl.ks.profiling.safepoint.analyzer.commons.shared.stringdedup.parser.StringDedupLogFile;
import pl.ks.profiling.safepoint.analyzer.commons.shared.thread.parser.ThreadLogFile;
import pl.ks.profiling.safepoint.analyzer.commons.shared.tlab.parser.TlabLogFile;

@Getter
@Setter
public class JvmLogFile {
    private UUID uuid;
    private String filename;

    private GCLogFile gcLogFile;
    private SafepointLogFile safepointLogFile;
    private ThreadLogFile threadLogFile;
    private TlabLogFile tlabLogFile;
    private ClassLoaderLogFile classLoaderLogFile;
    private JitLogFile jitLogFile;
    private StringDedupLogFile stringDedupLogFile;

    private List<Page> pages = new ArrayList<>();
}
