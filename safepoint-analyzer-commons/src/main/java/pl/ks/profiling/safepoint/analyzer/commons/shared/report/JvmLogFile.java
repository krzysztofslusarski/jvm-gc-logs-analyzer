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
package pl.ks.profiling.safepoint.analyzer.commons.shared.report;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;
import pl.ks.profiling.gui.commons.Page;
import pl.ks.profiling.safepoint.analyzer.commons.shared.classloader.parser.ClassLoaderLogFile;
import pl.ks.profiling.safepoint.analyzer.commons.shared.gc.parser.GCLogFile;
import pl.ks.profiling.safepoint.analyzer.commons.shared.jit.parser.JitLogFile;
import pl.ks.profiling.safepoint.analyzer.commons.shared.safepoint.parser.SafepointLogFile;
import pl.ks.profiling.safepoint.analyzer.commons.shared.stringdedup.parser.StringDedupLogFile;
import pl.ks.profiling.safepoint.analyzer.commons.shared.thread.parser.ThreadLogFile;
import pl.ks.profiling.safepoint.analyzer.commons.shared.tlab.parser.TlabLogFile;

@Getter
@Setter
public class JvmLogFile {
    private UUID uuid;
    private ParsingMetaData parsing;

    private GCLogFile gcLogFile;
    private SafepointLogFile safepointLogFile;
    private ThreadLogFile threadLogFile;
    private TlabLogFile tlabLogFile;
    private ClassLoaderLogFile classLoaderLogFile;
    private JitLogFile jitLogFile;
    private StringDedupLogFile stringDedupLogFile;

    private List<Page> pages = new ArrayList<>();
}
