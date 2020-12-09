/*
 * Copyright 2020 Krzysztof Slusarski
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package pl.ks.profiling.safepoint.analyzer.commons.shared.safepoint.parser;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import lombok.Builder;
import lombok.Value;
import pl.ks.profiling.safepoint.analyzer.commons.shared.OneFiledAllStats;
import pl.ks.profiling.safepoint.analyzer.commons.shared.OneFiledAllStatsUtil;

public class SafepointStatsCreator {
    static final BigDecimal TO_MS_MULTIPLIER = new BigDecimal(1000);
    private static final BigDecimal PERCENT_MULTIPLIER = new BigDecimal(100);
    private static final int NEW_SCALE = 2;

    public static SafepointOperationStats create(SafepointLogFile safepointLogFile) {
        SafepointOperationStats stats = new SafepointOperationStats();
        stats.setTts(createAllStats(safepointLogFile.getSafepoints(), operation -> operation.getTtsTime().multiply(TO_MS_MULTIPLIER).setScale(NEW_SCALE, RoundingMode.HALF_EVEN).doubleValue()));
        stats.setApplicationTime(createAllStats(safepointLogFile.getSafepoints(), operation -> operation.getApplicationTime().multiply(TO_MS_MULTIPLIER).setScale(NEW_SCALE, RoundingMode.HALF_EVEN).doubleValue()));
        stats.setOperationTime(createAllStats(safepointLogFile.getSafepoints(), operation -> operation.getStoppedTime().subtract(operation.getTtsTime()).multiply(TO_MS_MULTIPLIER).setScale(NEW_SCALE, RoundingMode.HALF_EVEN).doubleValue()));
        stats.setTotalCount(safepointLogFile.getSafepoints().size());
        stats.setTimesInTimes2sec(generateTimeStats(safepointLogFile.getSafepoints(), new BigDecimal("2")));
        stats.setTimesInTimes5sec(generateTimeStats(safepointLogFile.getSafepoints(), new BigDecimal("5")));
        stats.setTimesInTimes15sec(generateTimeStats(safepointLogFile.getSafepoints(), new BigDecimal("15")));
        Map<String, List<SafepointLogEntry>> statsByNameMap = new HashMap<>();
        safepointLogFile.getSafepoints().forEach(operation -> {
            List<SafepointLogEntry> operationsByName = statsByNameMap.computeIfAbsent(operation.getOperationName(), name -> new ArrayList<>());
            operationsByName.add(operation);
        });

        stats.setStatsByNames(new HashSet<>());
        for (Map.Entry<String, List<SafepointLogEntry>> entry : statsByNameMap.entrySet()) {
            List<SafepointLogEntry> operationsByName = entry.getValue();
            SafepointOperationStatsByName statsByName = new SafepointOperationStatsByName();
            statsByName.setOperationTime(createAllStats(operationsByName, operation -> operation.getStoppedTime()
                    .subtract(operation.getTtsTime())
                    .multiply(TO_MS_MULTIPLIER)
                    .setScale(NEW_SCALE, RoundingMode.HALF_EVEN).doubleValue()));
            statsByName.setCount(operationsByName.size());
            statsByName.setOperationName(entry.getKey());
            statsByName.setCountPercent(new BigDecimal(operationsByName.size())
                    .multiply(PERCENT_MULTIPLIER)
                    .divide(new BigDecimal(safepointLogFile.getSafepoints().size()), NEW_SCALE, RoundingMode.HALF_EVEN)
                    .setScale(NEW_SCALE, RoundingMode.HALF_EVEN));
            BigDecimal ttsTime = stats.getTts().getAverage()
                    .multiply(new BigDecimal(operationsByName.size()));
            statsByName.setTimeWithTtsPercent(statsByName.getOperationTime().getTotal()
                    .add(ttsTime)
                    .multiply(PERCENT_MULTIPLIER)
                    .divide(stats.getOperationTime().getTotal().add(stats.getTts().getTotal()), NEW_SCALE, RoundingMode.HALF_EVEN)
                    .setScale(NEW_SCALE, RoundingMode.HALF_EVEN));
            statsByName.setStatsByTime(generateInTimeStats(safepointLogFile.getSafepoints(), entry.getKey()));
            stats.getStatsByNames().add(statsByName);
        }
        return stats;
    }

    private static List<TimesInTime> generateTimeStats(List<SafepointLogEntry> operations, BigDecimal interval) {
        List<Phase> phases = new ArrayList<>(operations.size() * 3);
        for (SafepointLogEntry operation : operations) {
            phases.add(Phase.builder()
                    .time(operation.getApplicationTime())
                    .type(PhaseType.APPLICATION)
                    .build());
            phases.add(Phase.builder()
                    .time(operation.getTtsTime())
                    .type(PhaseType.TTS)
                    .build());
            phases.add(Phase.builder()
                    .time(operation.getStoppedTime().subtract(operation.getTtsTime()))
                    .type(PhaseType.SAFEPOINT)
                    .build());
        }

        if (operations.size() == 0) {
            return Collections.emptyList();
        }
        BigDecimal currentStartTime = operations.get(0).getTimeStamp();
        if (currentStartTime == null) {
            currentStartTime = BigDecimal.ZERO;
        }
        BigDecimal remainingTime = interval;
        BigDecimal currentEndTime = currentStartTime.add(interval);

        List<TimesInTime> timesInTimes = new ArrayList<>();
        TimesInTime current = new TimesInTime();
        current.setStartTime(currentStartTime);
        current.setEndTime(currentEndTime);

        for (Phase phase : phases) {
            BigDecimal phaseTime = phase.getTime();
            while (phaseTime.compareTo(remainingTime) >= 0) {
                switch (phase.getType()) {
                    case APPLICATION:
                        current.setApplicationTime(current.getApplicationTime().add(remainingTime));
                        break;
                    case SAFEPOINT:
                        current.setOperationTime(current.getOperationTime().add(remainingTime));
                        break;
                    case TTS:
                        current.setTts(current.getTts().add(remainingTime));
                        break;
                }
                phaseTime = phaseTime.subtract(remainingTime);
                remainingTime = interval;
                timesInTimes.add(current);
                currentStartTime = currentEndTime;
                currentEndTime = currentEndTime.add(interval);
                current = new TimesInTime();
                current.setStartTime(currentStartTime);
                current.setEndTime(currentEndTime);
            }
            switch (phase.getType()) {
                case APPLICATION:
                    current.setApplicationTime(current.getApplicationTime().add(phaseTime));
                    break;
                case SAFEPOINT:
                    current.setOperationTime(current.getOperationTime().add(phaseTime));
                    break;
                case TTS:
                    current.setTts(current.getTts().add(phaseTime));
                    break;
            }
            remainingTime = remainingTime.subtract(phaseTime);
        }

        return timesInTimes;
    }


    @Value
    @Builder
    private static class Phase {
        private BigDecimal time;
        private PhaseType type;
    }

    enum PhaseType {
        APPLICATION,
        SAFEPOINT,
        TTS
    }

    private static Set<SafepointInTimeStats> generateInTimeStats(List<SafepointLogEntry> operations, String name) {
        Set<SafepointInTimeStats> safepointInTimeStats = new HashSet<>();
        BigDecimal time = BigDecimal.ZERO;
        long count = 0;
        BigDecimal timeSpent = BigDecimal.ZERO;
        for (SafepointLogEntry operation : operations) {
            if (operation.getOperationName().equals(name)) {
                count++;
                timeSpent = timeSpent.add(operation.getStoppedTime().subtract(operation.getTtsTime()));
                SafepointInTimeStats stat = new SafepointInTimeStats();
                stat.setCount(count);
                stat.setTime(time);
                stat.setTimeSpent(timeSpent);
                safepointInTimeStats.add(stat);
            }
            time = time.add(operation.getApplicationTime()).add(operation.getStoppedTime());
        }
        return safepointInTimeStats;
    }

    private static OneFiledAllStats createAllStats(List<SafepointLogEntry> operations, Function<SafepointLogEntry, Double> valueFunc) {
        double[] values = operations.stream()
                .map(valueFunc)
                .mapToDouble(Double::doubleValue)
                .toArray();
        return OneFiledAllStatsUtil.create(values);
    }
}
