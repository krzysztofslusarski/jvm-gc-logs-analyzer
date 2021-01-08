/*
 * Copyright 2020 Krzysztof Slusarski, Artur Owczarek
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
package pl.ks.profiling.safepoint.analyzer.web;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.concurrent.CustomizableThreadFactory;
import org.springframework.stereotype.Component;
import pl.ks.profiling.safepoint.analyzer.commons.shared.JvmLogFile;
import pl.ks.profiling.safepoint.analyzer.commons.shared.ParsingProgress;
import pl.ks.profiling.safepoint.analyzer.commons.shared.StatsService;

import java.io.InputStream;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Slf4j
@Component
public class ParsingExecutorSimple implements ParsingExecutor {
    private final StatsService statsService;
    private final StatsRepository statsRepository;
    private final Cache<String, ParsingStatus> statuses;
    private final ExecutorService executor;

    public ParsingExecutorSimple(StatsService statsService, StatsRepository statsRepository, ParsingProperties parsingProperties) {
        this.statsService = statsService;
        this.statsRepository = statsRepository;
        this.statuses = CacheBuilder.newBuilder().expireAfterAccess(parsingProperties.results.expiration).build();
        this.executor = Executors.newFixedThreadPool(parsingProperties.workerThreads, new CustomizableThreadFactory("parsing-"));
    }

    public ParsingStatus enqueue(InputStream inputStream, String originalFilename) {
        String parsingId = UUID.randomUUID().toString();
        executor.submit(() -> {
            log.info("Submitting parsing {} to parser", parsingId);
            statsService.createAllStatsUnifiedLogger(
                    inputStream,
                    originalFilename,
                    (ParsingProgress p) -> updateParsingProgress(parsingId, p),
                    (JvmLogFile f) -> storeInRepo(parsingId, f));
        });
        return new ParsingStatus(parsingId, 0, false);
    }

    private void updateParsingProgress(String parsingId, ParsingProgress progress) {
        statuses.put(parsingId, new ParsingStatus(parsingId, progress.getProcessedLines(), progress.isCompleted()));
    }

    private void storeInRepo(String parsingId, JvmLogFile f) {
        log.info("Storing parsing {} in repository", parsingId);
        statsRepository.put(parsingId, f);
    }

    public ParsingStatus getParsingStatus(String parsingId) {
        return statuses.getIfPresent(parsingId);
    }
}
