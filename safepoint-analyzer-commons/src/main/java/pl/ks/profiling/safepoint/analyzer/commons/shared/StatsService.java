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
import pl.ks.profiling.safepoint.analyzer.commons.shared.page.ClassCount;
import pl.ks.profiling.safepoint.analyzer.commons.shared.page.ClassLoading;
import pl.ks.profiling.safepoint.analyzer.commons.shared.page.GCAllocationRate;
import pl.ks.profiling.safepoint.analyzer.commons.shared.page.GCAllocationRateInTime;
import pl.ks.profiling.safepoint.analyzer.commons.shared.page.GCHeapAfter;
import pl.ks.profiling.safepoint.analyzer.commons.shared.page.GCHeapBefore;
import pl.ks.profiling.safepoint.analyzer.commons.shared.page.GCPhaseTime;
import pl.ks.profiling.safepoint.analyzer.commons.shared.page.GCRegionCountAfter;
import pl.ks.profiling.safepoint.analyzer.commons.shared.page.GCRegionCountBefore;
import pl.ks.profiling.safepoint.analyzer.commons.shared.page.GCRegionCountBeforeAndAfter;
import pl.ks.profiling.safepoint.analyzer.commons.shared.page.GCRegionMax;
import pl.ks.profiling.safepoint.analyzer.commons.shared.page.GCRegionSizeAfter;
import pl.ks.profiling.safepoint.analyzer.commons.shared.page.GCSubphaseStats;
import pl.ks.profiling.safepoint.analyzer.commons.shared.page.GCTableStats;
import pl.ks.profiling.safepoint.analyzer.commons.shared.page.JitCodeCacheStats;
import pl.ks.profiling.safepoint.analyzer.commons.shared.page.JitCodeCacheSweeperActivity;
import pl.ks.profiling.safepoint.analyzer.commons.shared.page.JitCompilationCount;
import pl.ks.profiling.safepoint.analyzer.commons.shared.page.JitTieredCompilationCount;
import pl.ks.profiling.safepoint.analyzer.commons.shared.page.PageCreator;
import pl.ks.profiling.safepoint.analyzer.commons.shared.page.SafepoinOperationCount;
import pl.ks.profiling.safepoint.analyzer.commons.shared.page.SafepoinOperationTime;
import pl.ks.profiling.safepoint.analyzer.commons.shared.page.SafepointApplicationTimeByTime15Sec;
import pl.ks.profiling.safepoint.analyzer.commons.shared.page.SafepointApplicationTimeByTime2Sec;
import pl.ks.profiling.safepoint.analyzer.commons.shared.page.SafepointApplicationTimeByTime5Sec;
import pl.ks.profiling.safepoint.analyzer.commons.shared.page.SafepointOperationTimeCharts;
import pl.ks.profiling.safepoint.analyzer.commons.shared.page.SafepointTableStats;
import pl.ks.profiling.safepoint.analyzer.commons.shared.page.SafepointTotalTimeInPhases;
import pl.ks.profiling.safepoint.analyzer.commons.shared.page.StringDedupLast;
import pl.ks.profiling.safepoint.analyzer.commons.shared.page.StringDedupTotal;
import pl.ks.profiling.safepoint.analyzer.commons.shared.page.ThreadCount;
import pl.ks.profiling.safepoint.analyzer.commons.shared.page.ThreadCreation;
import pl.ks.profiling.safepoint.analyzer.commons.shared.page.TlabSummary;
import pl.ks.profiling.safepoint.analyzer.commons.shared.page.TlabThreadStats;
import pl.ks.profiling.safepoint.analyzer.commons.shared.pareser.classloader.ClassLoaderLogFileParser;
import pl.ks.profiling.safepoint.analyzer.commons.shared.pareser.gc.GcLogFileParser;
import pl.ks.profiling.safepoint.analyzer.commons.shared.pareser.gc.GcStatsCreator;
import pl.ks.profiling.safepoint.analyzer.commons.shared.pareser.jit.JitLogFileParser;
import pl.ks.profiling.safepoint.analyzer.commons.shared.pareser.safepoint.JvmLogFile;
import pl.ks.profiling.safepoint.analyzer.commons.shared.pareser.safepoint.SafepointLogFileParser;
import pl.ks.profiling.safepoint.analyzer.commons.shared.pareser.stringdedup.StringDedupLogFileParser;
import pl.ks.profiling.safepoint.analyzer.commons.shared.pareser.thread.ThreadLogFileParser;
import pl.ks.profiling.safepoint.analyzer.commons.shared.pareser.tlab.TlabLogFileParser;

@RequiredArgsConstructor
public class StatsService {
    public JvmLogFile createAllStats(InputStream inputStream, String originalFilename) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
        SafepointLogFileParser safepointLogFileParser = new SafepointLogFileParser();
        GcLogFileParser gcLogFileParser = new GcLogFileParser();
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
        jvmLogFile.setGcStats(GcStatsCreator.createStats(jvmLogFile.getGcLogFile()));
        jvmLogFile.setThreadLogFile(threadLogFileParser.fetchData());
        jvmLogFile.setClassLoaderLogFile(classLoaderLogFileParser.fetchData());
        jvmLogFile.setJitLogFile(jitLogFileParser.fetchData());
        jvmLogFile.setTlabLogFile(tlabLogFileParser.fetchData());
        jvmLogFile.setStringDedupLogFile(stringDedupLogFileParser.fetchData());

        addPages(jvmLogFile);
        return jvmLogFile;
    }

    private void addPages(JvmLogFile jvmLogFile) {
        DecimalFormat decimalFormat = new DecimalFormat("#,##0.00", DecimalFormatSymbols.getInstance(Locale.US));
        createSafepointPages(jvmLogFile, decimalFormat);
        createGcPages(jvmLogFile, decimalFormat);
        createThreadPages(jvmLogFile, decimalFormat);
        createClassLoaderPages(jvmLogFile, decimalFormat);
        createJitPages(jvmLogFile, decimalFormat);
        createTlabPages(jvmLogFile, decimalFormat);
        createStringDedupPages(jvmLogFile, decimalFormat);
    }

    private void createStringDedupPages(JvmLogFile jvmLogFile, DecimalFormat decimalFormat) {
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

    private void createTlabPages(JvmLogFile jvmLogFile, DecimalFormat decimalFormat) {
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

    private void createJitPages(JvmLogFile jvmLogFile, DecimalFormat decimalFormat) {
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

    private void createClassLoaderPages(JvmLogFile jvmLogFile, DecimalFormat decimalFormat) {
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

    private void createThreadPages(JvmLogFile jvmLogFile, DecimalFormat decimalFormat) {
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

    private static void createGcPages(JvmLogFile jvmLogFile, DecimalFormat decimalFormat) {
        if (CollectionUtils.isEmpty(jvmLogFile.getGcLogFile().getGcCycleInfos())) {
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

    private static void createSafepointPages(JvmLogFile jvmLogFile, DecimalFormat decimalFormat) {
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
