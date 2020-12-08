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
import pl.ks.profiling.safepoint.analyzer.commons.shared.pareser.safepoint.SafepointLogFile;
import pl.ks.profiling.safepoint.analyzer.commons.shared.pareser.safepoint.SafepointLogFileParser;
import pl.ks.profiling.safepoint.analyzer.commons.shared.pareser.safepoint.SafepointStatsCreator;
import pl.ks.profiling.safepoint.analyzer.commons.shared.pareser.stringdedup.StringDedupLogFileParser;
import pl.ks.profiling.safepoint.analyzer.commons.shared.pareser.thread.ThreadLogFileParser;
import pl.ks.profiling.safepoint.analyzer.commons.shared.pareser.tlab.TlabLogFileParser;

@RequiredArgsConstructor
public class StatsService {
    public SafepointLogFile createAllStats(InputStream inputStream, String originalFilename) throws IOException {
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

        SafepointLogFile safepointLogFile = new SafepointLogFile();
        safepointLogFile.setFilename(originalFilename);
        safepointLogFile.setSafepointOperations(safepointLogFileParser.fetchData());
        safepointLogFile.setSafepointOperationStats(SafepointStatsCreator.create(safepointLogFile.getSafepointOperations()));
        safepointLogFile.setGcLogFile(gcLogFileParser.fetchData());
        safepointLogFile.setGcStats(GcStatsCreator.createStats(safepointLogFile.getGcLogFile()));
        safepointLogFile.setThreadLogFile(threadLogFileParser.fetchData());
        safepointLogFile.setClassLoaderLogFile(classLoaderLogFileParser.fetchData());
        safepointLogFile.setJitLogFile(jitLogFileParser.fetchData());
        safepointLogFile.setTlabLogFile(tlabLogFileParser.fetchData());
        safepointLogFile.setStringDedupLogFile(stringDedupLogFileParser.fetchData());

        addPages(safepointLogFile);
        return safepointLogFile;
    }

    private void addPages(SafepointLogFile safepointLogFile) {
        DecimalFormat decimalFormat = new DecimalFormat("#,##0.00", DecimalFormatSymbols.getInstance(Locale.US));
        createSafepointPages(safepointLogFile, decimalFormat);
        createGcPages(safepointLogFile, decimalFormat);
        createThreadPages(safepointLogFile, decimalFormat);
        createClassLoaderPages(safepointLogFile, decimalFormat);
        createJitPages(safepointLogFile, decimalFormat);
        createTlabPages(safepointLogFile, decimalFormat);
        createStringDedupPages(safepointLogFile, decimalFormat);
    }

    private void createStringDedupPages(SafepointLogFile safepointLogFile, DecimalFormat decimalFormat) {
        List<PageCreator> stringDedupPageCreators = new ArrayList<>();
        if (!safepointLogFile.getStringDedupLogFile().getEntries().isEmpty()) {
            stringDedupPageCreators.add(new StringDedupTotal());
            stringDedupPageCreators.add(new StringDedupLast());
        }

        for (PageCreator stringDedupPageCreator : stringDedupPageCreators) {
            Page page = stringDedupPageCreator.create(safepointLogFile, decimalFormat);
            if (page != null) {
                safepointLogFile.getPages().add(page);
            }
        }
    }

    private void createTlabPages(SafepointLogFile safepointLogFile, DecimalFormat decimalFormat) {
        List<PageCreator> tlabPageCreators = new ArrayList<>();
        if (!safepointLogFile.getTlabLogFile().getTlabSummaries().isEmpty()) {
            tlabPageCreators.add(new TlabSummary());
        }

        if (!safepointLogFile.getTlabLogFile().getThreadTlabsBeforeGC().isEmpty()) {
            tlabPageCreators.add(new TlabThreadStats());
        }

        for (PageCreator jitPageCreator : tlabPageCreators) {
            Page page = jitPageCreator.create(safepointLogFile, decimalFormat);
            if (page != null) {
                safepointLogFile.getPages().add(page);
            }
        }
    }

    private void createJitPages(SafepointLogFile safepointLogFile, DecimalFormat decimalFormat) {
        List<PageCreator> jitPageCreators = new ArrayList<>();
        if (safepointLogFile.getJitLogFile().getLastStatus() != null) {
            jitPageCreators.add(new JitCompilationCount());
            jitPageCreators.add(new JitTieredCompilationCount());
        }

        if (safepointLogFile.getJitLogFile().getCodeCacheStatuses().size() > 0) {
            jitPageCreators.add(new JitCodeCacheStats());
        }

        if (safepointLogFile.getJitLogFile().getCodeCacheSweeperActivities().size() > 0) {
            jitPageCreators.add(new JitCodeCacheSweeperActivity());
        }

        for (PageCreator jitPageCreator : jitPageCreators) {
            Page page = jitPageCreator.create(safepointLogFile, decimalFormat);
            if (page != null) {
                safepointLogFile.getPages().add(page);
            }
        }
    }

    private void createClassLoaderPages(SafepointLogFile safepointLogFile, DecimalFormat decimalFormat) {
        if (safepointLogFile.getClassLoaderLogFile().getLastStatus() == null) {
            return;
        }

        List<PageCreator> classLoaderPageCreators = List.of(
                new ClassCount(),
                new ClassLoading()
        );

        for (PageCreator classLoaderPageCreator : classLoaderPageCreators) {
            Page page = classLoaderPageCreator.create(safepointLogFile, decimalFormat);
            if (page != null) {
                safepointLogFile.getPages().add(page);
            }
        }
    }

    private void createThreadPages(SafepointLogFile safepointLogFile, DecimalFormat decimalFormat) {
        if (safepointLogFile.getThreadLogFile().getLastStatus() == null) {
            return;
        }

        List<PageCreator> threadPageCreators = List.of(
                new ThreadCount(),
                new ThreadCreation()
        );

        for (PageCreator threadPageCreator : threadPageCreators) {
            Page page = threadPageCreator.create(safepointLogFile, decimalFormat);
            if (page != null) {
                safepointLogFile.getPages().add(page);
            }
        }
    }

    private static void createGcPages(SafepointLogFile safepointLogFile, DecimalFormat decimalFormat) {
        if (CollectionUtils.isEmpty(safepointLogFile.getGcLogFile().getGcCycleInfos())) {
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
            Page page = safepointPageCreator.create(safepointLogFile, decimalFormat);
            if (page != null) {
                safepointLogFile.getPages().add(page);
            }
        }
    }

    private static void createSafepointPages(SafepointLogFile safepointLogFile, DecimalFormat decimalFormat) {
        if (CollectionUtils.isEmpty(safepointLogFile.getSafepointOperations())) {
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
            safepointLogFile.getPages().add(safepointPageCreator.create(safepointLogFile, decimalFormat));
        }
    }
}
