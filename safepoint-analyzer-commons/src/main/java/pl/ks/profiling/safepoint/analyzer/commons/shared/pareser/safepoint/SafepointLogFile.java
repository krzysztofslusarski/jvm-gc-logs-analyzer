package pl.ks.profiling.safepoint.analyzer.commons.shared.pareser.safepoint;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
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
public class SafepointLogFile {
    private Long id;
    private UUID uuid;
    private SafepointOperationStats safepointOperationStats;
    private List<SafepointOperation> safepointOperations;
    private GcLogFile gcLogFile;
    private GcStats gcStats;
    private String filename;
    private ThreadLogFile threadLogFile;
    private TlabLogFile tlabLogFile;
    private ClassLoaderLogFile classLoaderLogFile;
    private JitLogFile jitLogFile;
    private StringDedupLogFile stringDedupLogFile;
    private List<Page> pages = new ArrayList<>();

    public List<Object[][]> getInTimeChart() {
        Set<SafepointOperationStatsByName> statsByName = safepointOperationStats.getStatsByNames();

        List<Object[][]> inTimeList = new ArrayList<>();

        for (SafepointOperationStatsByName operationStatsByName : statsByName) {
            Object[][] stats = new Object[operationStatsByName.getStatsByTime().size() + 1][2];
            stats[0][0] = "Time";
            stats[0][1] = operationStatsByName.getOperationName();
            int i = 1;
            List<SafepointInTimeStats> sorted = operationStatsByName.getStatsByTime().stream()
                    .sorted(Comparator.comparing(SafepointInTimeStats::getTime))
                    .collect(Collectors.toList());
            for (SafepointInTimeStats inTimeStats : sorted) {
                stats[i][0] = inTimeStats.getTime().longValue();
                stats[i][1] = inTimeStats.getCount();
                i++;
            }

            inTimeList.add(stats);
        }

        return inTimeList;
    }

    public List<Object[][]> getTimeSpentInTimeChart() {
        Set<SafepointOperationStatsByName> statsByName = safepointOperationStats.getStatsByNames();

        List<Object[][]> inTimeList = new ArrayList<>();

        for (SafepointOperationStatsByName operationStatsByName : statsByName) {
            Object[][] stats = new Object[operationStatsByName.getStatsByTime().size() + 1][2];
            stats[0][0] = "Time";
            stats[0][1] = operationStatsByName.getOperationName();
            int i = 1;
            List<SafepointInTimeStats> sorted = operationStatsByName.getStatsByTime().stream()
                    .sorted(Comparator.comparing(SafepointInTimeStats::getTime))
                    .collect(Collectors.toList());

            for (SafepointInTimeStats inTimeStats : narrow(sorted, 500)) {
                stats[i][0] = inTimeStats.getTime().longValue();
                stats[i][1] = inTimeStats.getTimeSpent();
                i++;
            }
            inTimeList.add(stats);
        }

        return inTimeList;
    }

    @SuppressWarnings("ConstantConditions")
    private <Arg> Collection<Arg> narrow(Collection<Arg> inputList, int size) {
        if (inputList.size() <= size) {
            return inputList;
        }
        int prevValue = -1;
        double currentValue = 0;
        double addedValue = (double) size / (double) inputList.size();

        List<Arg> ret = new ArrayList<>(size + 2);

        for (Arg arg : inputList) {
            if ((int) currentValue != prevValue) {
                prevValue = (int) currentValue;
                ret.add(arg);
            }
            currentValue += addedValue;
        }
        return ret;
    }
}
