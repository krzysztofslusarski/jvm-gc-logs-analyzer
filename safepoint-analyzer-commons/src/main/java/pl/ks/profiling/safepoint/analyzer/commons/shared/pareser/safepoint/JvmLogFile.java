package pl.ks.profiling.safepoint.analyzer.commons.shared.pareser.safepoint;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;
import pl.ks.profiling.gui.commons.Page;
import pl.ks.profiling.safepoint.analyzer.commons.shared.pareser.classloader.ClassLoaderLogFile;
import pl.ks.profiling.safepoint.analyzer.commons.shared.pareser.gc.GcLogFile;
import pl.ks.profiling.safepoint.analyzer.commons.shared.pareser.gc.GcStats;
import pl.ks.profiling.safepoint.analyzer.commons.shared.pareser.jit.JitLogFile;
import pl.ks.profiling.safepoint.analyzer.commons.shared.pareser.stringdedup.StringDedupLogFile;
import pl.ks.profiling.safepoint.analyzer.commons.shared.pareser.thread.ThreadLogFile;
import pl.ks.profiling.safepoint.analyzer.commons.shared.pareser.tlab.TlabLogFile;

@Getter
@Setter
public class JvmLogFile {
    private Long id;
    private UUID uuid;
    private GcLogFile gcLogFile;
    private GcStats gcStats;
    private String filename;
    private SafepointLogFile safepointLogFile;
    private ThreadLogFile threadLogFile;
    private TlabLogFile tlabLogFile;
    private ClassLoaderLogFile classLoaderLogFile;
    private JitLogFile jitLogFile;
    private StringDedupLogFile stringDedupLogFile;
    private List<Page> pages = new ArrayList<>();
}
