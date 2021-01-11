/*
 * Copyright 2020 Artur Owczarek
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
import org.springframework.stereotype.Repository;
import pl.ks.profiling.safepoint.analyzer.commons.shared.report.JvmLogFile;

@Slf4j
@Repository
public class InMemoryStatsRepository implements StatsRepository {
    private final ParsingProperties parsingProperties;
    private final Cache<String, JvmLogFile> results;

    public InMemoryStatsRepository(ParsingProperties parsingProperties) {
        this.parsingProperties = parsingProperties;
        results = CacheBuilder.newBuilder().expireAfterAccess(parsingProperties.results.expiration).build();
    }

    @Override
    public JvmLogFile get(String parsingId) {
        log.trace("Getting parsing {}", parsingId);
        JvmLogFile result = results.getIfPresent(parsingId);
        if (parsingProperties.results.removeAfterRead) {
            results.invalidate(parsingId);
        }
        return result;
    }

    @Override
    public void put(String parsingId, JvmLogFile parsingResult) {
        log.debug("Storing parsing with id {}", parsingId);
        results.put(parsingId, parsingResult);
    }
}
