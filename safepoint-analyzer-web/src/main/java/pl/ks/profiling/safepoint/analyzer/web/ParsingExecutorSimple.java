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
import java.util.function.Function;

@Slf4j
@Component
public class ParsingExecutorSimple implements ParsingExecutor {
    private final StatsService statsService;
    private final StatsRepository statsRepository;
    private final Cache<String, ParsingStatus> statuses;
    private final ExecutorService executor;
    private final ParsingProperties parsingProperties;

    public ParsingExecutorSimple(StatsService statsService, StatsRepository statsRepository, ParsingProperties parsingProperties) {
        this.statsService = statsService;
        this.statsRepository = statsRepository;
        this.parsingProperties = parsingProperties;
        this.statuses = CacheBuilder.newBuilder().expireAfterAccess(parsingProperties.results.expiration).build();
        this.executor = Executors.newFixedThreadPool(parsingProperties.workerThreads, new CustomizableThreadFactory("parsing-"));
    }

    public ParsingStatus enqueue(InputStream inputStream, String originalFilename, Function<String, String> resultLocationFactory) {
        String parsingId = UUID.randomUUID().toString();
        executor.submit(() -> {
            log.info("Submitting parsing {} to parser", parsingId);
            statsService.createAllStatsUnifiedLogger(
                    inputStream,
                    originalFilename,
                    (ParsingProgress p) -> updateParsingProgress(parsingId, p),
                    (JvmLogFile f) -> storeInRepo(parsingId, f));
        });
        ParsingStatus parsingStatus = createParsingInitialParsingStatus(resultLocationFactory, parsingId);
        statuses.put(parsingId, parsingStatus);
        return parsingStatus;
    }

    private void updateParsingProgress(String parsingId, ParsingProgress progress) {
        ParsingStatus current = statuses.getIfPresent(parsingId);
        if (current != null) {
            ParsingStatus updated = current
                    .withFinished(progress.isCompleted())
                    .withProcessedLines(progress.getProcessedLines());
            statuses.put(parsingId, updated);
        } else {
            log.warn("Status for parsing {} is not available", parsingId);
        }
    }

    private void storeInRepo(String parsingId, JvmLogFile f) {
        log.info("Storing parsing {} in repository", parsingId);
        statsRepository.put(parsingId, f);
    }

    private ParsingStatus createParsingInitialParsingStatus(Function<String, String> resultLocationFactory, String parsingId) {
        return new ParsingStatus(
                parsingId,
                resultLocationFactory.apply(parsingId),
                this.parsingProperties.results.expiration.toMinutes(),
                0,
                false);
    }

    public ParsingStatus getParsingStatus(String parsingId) {
        return statuses.getIfPresent(parsingId);
    }
}
