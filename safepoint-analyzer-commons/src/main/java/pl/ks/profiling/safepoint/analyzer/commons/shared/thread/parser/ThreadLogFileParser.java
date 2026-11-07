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
package pl.ks.profiling.safepoint.analyzer.commons.shared.thread.parser;

import pl.ks.profiling.safepoint.analyzer.commons.FileParser;
import pl.ks.profiling.safepoint.analyzer.commons.shared.ParserUtils;
import java.util.regex.Pattern;

public class ThreadLogFileParser implements FileParser<ThreadLogFile> {
    private static final Pattern THREAD_STARTED_PATTERN = Pattern.compile("Thread \".*\" started");

    private final ThreadLogFile threadLogFile = new ThreadLogFile();

    public ThreadLogFileParser() {
    }

    @Override
    public void parseLine(String line) {
        if (!line.contains("os,thread")) {
            return;
        }

        // A new thread is logged by its creator: 'Thread "name" started (pthread id: ...)' (older JDKs:
        // "Thread started"); threads created outside of the JVM (e.g. main) log "Thread attached".
        // "Thread is alive" is logged by the new thread itself, so it is not counted again.
        if (line.contains("Thread started") || THREAD_STARTED_PATTERN.matcher(line).find() || line.contains("Thread attached")) {
            threadLogFile.newThreadAdded(ParserUtils.getTimeStamp(line));
        } else if (line.contains("Thread finished")) {
            threadLogFile.threadDestroyed(ParserUtils.getTimeStamp(line));
        }
    }

    @Override
    public ThreadLogFile fetchData() {
        return threadLogFile;
    }
}
