package pl.ks.profiling.safepoint.analyzer.commons.shared;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.apache.commons.collections4.CollectionUtils;
import pl.ks.profiling.gui.commons.Page;
import pl.ks.profiling.safepoint.analyzer.commons.shared.classloader.page.ClassCount;
import pl.ks.profiling.safepoint.analyzer.commons.shared.classloader.page.ClassLoading;
import pl.ks.profiling.safepoint.analyzer.commons.shared.classloader.parser.ClassLoaderLogFileParser;
import pl.ks.profiling.safepoint.analyzer.commons.shared.gc.page.GCAllocationRate;
import pl.ks.profiling.safepoint.analyzer.commons.shared.gc.page.GCAllocationRateInTime;
import pl.ks.profiling.safepoint.analyzer.commons.shared.gc.page.GCHeapAfter;
import pl.ks.profiling.safepoint.analyzer.commons.shared.gc.page.GCHeapBefore;
import pl.ks.profiling.safepoint.analyzer.commons.shared.gc.page.GCPhaseTime;
import pl.ks.profiling.safepoint.analyzer.commons.shared.gc.page.GCRegionCountAfter;
import pl.ks.profiling.safepoint.analyzer.commons.shared.gc.page.GCRegionCountBefore;
import pl.ks.profiling.safepoint.analyzer.commons.shared.gc.page.GCRegionCountBeforeAndAfter;
import pl.ks.profiling.safepoint.analyzer.commons.shared.gc.page.GCRegionMax;
import pl.ks.profiling.safepoint.analyzer.commons.shared.gc.page.GCRegionSizeAfter;
import pl.ks.profiling.safepoint.analyzer.commons.shared.gc.page.GCSubphaseStats;
import pl.ks.profiling.safepoint.analyzer.commons.shared.gc.page.GCTableStats;
import pl.ks.profiling.safepoint.analyzer.commons.shared.gc.parser.GCLogFileParser;
import pl.ks.profiling.safepoint.analyzer.commons.shared.jit.page.JitCodeCacheStats;
import pl.ks.profiling.safepoint.analyzer.commons.shared.jit.page.JitCodeCacheSweeperActivity;
import pl.ks.profiling.safepoint.analyzer.commons.shared.jit.page.JitCompilationCount;
import pl.ks.profiling.safepoint.analyzer.commons.shared.jit.page.JitTieredCompilationCount;
import pl.ks.profiling.safepoint.analyzer.commons.shared.jit.parser.JitLogFileParser;
import pl.ks.profiling.safepoint.analyzer.commons.shared.safepoint.page.SafepoinOperationCount;
import pl.ks.profiling.safepoint.analyzer.commons.shared.safepoint.page.SafepoinOperationTime;
import pl.ks.profiling.safepoint.analyzer.commons.shared.safepoint.page.SafepointApplicationTimeByTime15Sec;
import pl.ks.profiling.safepoint.analyzer.commons.shared.safepoint.page.SafepointApplicationTimeByTime2Sec;
import pl.ks.profiling.safepoint.analyzer.commons.shared.safepoint.page.SafepointApplicationTimeByTime5Sec;
import pl.ks.profiling.safepoint.analyzer.commons.shared.safepoint.page.SafepointOperationTimeCharts;
import pl.ks.profiling.safepoint.analyzer.commons.shared.safepoint.page.SafepointTableStats;
import pl.ks.profiling.safepoint.analyzer.commons.shared.safepoint.page.SafepointTotalTimeInPhases;
import pl.ks.profiling.safepoint.analyzer.commons.shared.safepoint.parser.SafepointLogFileParser;
import pl.ks.profiling.safepoint.analyzer.commons.shared.stringdedup.page.StringDedupLast;
import pl.ks.profiling.safepoint.analyzer.commons.shared.stringdedup.page.StringDedupTotal;
import pl.ks.profiling.safepoint.analyzer.commons.shared.stringdedup.parser.StringDedupLogFileParser;
import pl.ks.profiling.safepoint.analyzer.commons.shared.thread.page.ThreadCount;
import pl.ks.profiling.safepoint.analyzer.commons.shared.thread.page.ThreadCreation;
import pl.ks.profiling.safepoint.analyzer.commons.shared.thread.parser.ThreadLogFileParser;
import pl.ks.profiling.safepoint.analyzer.commons.shared.tlab.page.TlabSummary;
import pl.ks.profiling.safepoint.analyzer.commons.shared.tlab.page.TlabThreadStats;
import pl.ks.profiling.safepoint.analyzer.commons.shared.tlab.parser.TlabLogFileParser;

@RequiredArgsConstructor
public class StatsService {
    private DecimalFormat decimalFormat = new DecimalFormat("#,##0.00", DecimalFormatSymbols.getInstance(Locale.US));

    public JvmLogFile createAllStats(InputStream inputStream, String originalFilename) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
        SafepointLogFileParser safepointLogFileParser = new SafepointLogFileParser();
        GCLogFileParser gcLogFileParser = new GCLogFileParser();
        ThreadLogFileParser threadLogFileParser = new ThreadLogFileParser();
        ClassLoaderLogFileParser classLoaderLogFileParser = new ClassLoaderLogFileParser();
        JitLogFileParser jitLogFileParser = new JitLogFileParser();
        TlabLogFileParser tlabLogFileParser = new TlabLogFileParser();
        StringDedupLogFileParser stringDedupLogFileParser = new StringDedupLogFileParser();

        while (reader.ready()) {
            String line = reader.readLine();
            safepointLogFileParser.parseLine(line);
            gcLogFileParser.parseLine(line);
            threadLogFileParser.parseLine(line);
            classLoaderLogFileParser.parseLine(line);
            jitLogFileParser.parseLine(line);
            tlabLogFileParser.parseLine(line);
            stringDedupLogFileParser.parseLine(line);
        }

        JvmLogFile jvmLogFile = new JvmLogFile();
        jvmLogFile.setFilename(originalFilename);
        jvmLogFile.setSafepointLogFile(safepointLogFileParser.fetchData());
        jvmLogFile.setGcLogFile(gcLogFileParser.fetchData());
//        jvmLogFile.setGcStats(GcStatsCreator.createStats(jvmLogFile.getGcLogFile()));
        jvmLogFile.setThreadLogFile(threadLogFileParser.fetchData());
        jvmLogFile.setClassLoaderLogFile(classLoaderLogFileParser.fetchData());
        jvmLogFile.setJitLogFile(jitLogFileParser.fetchData());
        jvmLogFile.setTlabLogFile(tlabLogFileParser.fetchData());
        jvmLogFile.setStringDedupLogFile(stringDedupLogFileParser.fetchData());

        addPages(jvmLogFile);
        return jvmLogFile;
    }

    private void addPages(JvmLogFile jvmLogFile) {
        createSafepointPages(jvmLogFile);
        createGcPages(jvmLogFile);
        createThreadPages(jvmLogFile);
        createClassLoaderPages(jvmLogFile);
        createJitPages(jvmLogFile);
        createTlabPages(jvmLogFile);
        createStringDedupPages(jvmLogFile);
    }

    private void createStringDedupPages(JvmLogFile jvmLogFile) {
        List<PageCreator> stringDedupPageCreators = new ArrayList<>();
        if (!jvmLogFile.getStringDedupLogFile().getEntries().isEmpty()) {
            stringDedupPageCreators.add(new StringDedupTotal());
            stringDedupPageCreators.add(new StringDedupLast());
        }

        for (PageCreator stringDedupPageCreator : stringDedupPageCreators) {
            Page page = stringDedupPageCreator.create(jvmLogFile, decimalFormat);
            if (page != null) {
                jvmLogFile.getPages().add(page);
            }
        }
    }

    private void createTlabPages(JvmLogFile jvmLogFile) {
        List<PageCreator> tlabPageCreators = new ArrayList<>();
        if (!jvmLogFile.getTlabLogFile().getTlabSummaries().isEmpty()) {
            tlabPageCreators.add(new TlabSummary());
        }

        if (!jvmLogFile.getTlabLogFile().getThreadTlabsBeforeGC().isEmpty()) {
            tlabPageCreators.add(new TlabThreadStats());
        }

        for (PageCreator jitPageCreator : tlabPageCreators) {
            Page page = jitPageCreator.create(jvmLogFile, decimalFormat);
            if (page != null) {
                jvmLogFile.getPages().add(page);
            }
        }
    }

    private void createJitPages(JvmLogFile jvmLogFile) {
        List<PageCreator> jitPageCreators = new ArrayList<>();
        if (jvmLogFile.getJitLogFile().getLastStatus() != null) {
            jitPageCreators.add(new JitCompilationCount());
            jitPageCreators.add(new JitTieredCompilationCount());
        }

        if (jvmLogFile.getJitLogFile().getCodeCacheStatuses().size() > 0) {
            jitPageCreators.add(new JitCodeCacheStats());
        }

        if (jvmLogFile.getJitLogFile().getCodeCacheSweeperActivities().size() > 0) {
            jitPageCreators.add(new JitCodeCacheSweeperActivity());
        }

        for (PageCreator jitPageCreator : jitPageCreators) {
            Page page = jitPageCreator.create(jvmLogFile, decimalFormat);
            if (page != null) {
                jvmLogFile.getPages().add(page);
            }
        }
    }

    private void createClassLoaderPages(JvmLogFile jvmLogFile) {
        if (jvmLogFile.getClassLoaderLogFile().getLastStatus() == null) {
            return;
        }

        List<PageCreator> classLoaderPageCreators = List.of(
                new ClassCount(),
                new ClassLoading()
        );

        for (PageCreator classLoaderPageCreator : classLoaderPageCreators) {
            Page page = classLoaderPageCreator.create(jvmLogFile, decimalFormat);
            if (page != null) {
                jvmLogFile.getPages().add(page);
            }
        }
    }

    private void createThreadPages(JvmLogFile jvmLogFile) {
        if (jvmLogFile.getThreadLogFile().getLastStatus() == null) {
            return;
        }

        List<PageCreator> threadPageCreators = List.of(
                new ThreadCount(),
                new ThreadCreation()
        );

        for (PageCreator threadPageCreator : threadPageCreators) {
            Page page = threadPageCreator.create(jvmLogFile, decimalFormat);
            if (page != null) {
                jvmLogFile.getPages().add(page);
            }
        }
    }

    private void createGcPages(JvmLogFile jvmLogFile) {
        if (CollectionUtils.isEmpty(jvmLogFile.getGcLogFile().getCycleEntries())) {
            return;
        }
        List<PageCreator> gcPageCreators = List.of(
                new GCTableStats(),
                new GCSubphaseStats(),
                new GCPhaseTime(),
                new GCRegionCountBefore(),
                new GCRegionCountAfter(),
                new GCRegionCountBeforeAndAfter(),
                new GCRegionMax(),
                new GCRegionSizeAfter(),
                new GCHeapBefore(),
                new GCHeapAfter(),
                new GCAllocationRate(),
                new GCAllocationRateInTime(new BigDecimal(1)),
                new GCAllocationRateInTime(new BigDecimal(10))
        );

        for (PageCreator safepointPageCreator : gcPageCreators) {
            Page page = safepointPageCreator.create(jvmLogFile, decimalFormat);
            if (page != null) {
                jvmLogFile.getPages().add(page);
            }
        }
    }

    private void createSafepointPages(JvmLogFile jvmLogFile) {
        if (CollectionUtils.isEmpty(jvmLogFile.getSafepointLogFile().getSafepoints())) {
            return;
        }

        List<PageCreator> safepointPageCreators = List.of(
                new SafepointTableStats(),
                new SafepointTotalTimeInPhases(),
                new SafepointApplicationTimeByTime2Sec(),
                new SafepointApplicationTimeByTime5Sec(),
                new SafepointApplicationTimeByTime15Sec(),
                new SafepoinOperationCount(),
                new SafepoinOperationTime(),
                new SafepointOperationTimeCharts()
        );

        for (PageCreator safepointPageCreator : safepointPageCreators) {
            jvmLogFile.getPages().add(safepointPageCreator.create(jvmLogFile, decimalFormat));
        }
    }
}
