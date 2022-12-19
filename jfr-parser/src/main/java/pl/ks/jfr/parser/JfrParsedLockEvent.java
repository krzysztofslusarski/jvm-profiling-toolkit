/*
 * Copyright 2022 Krzysztof Slusarski
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
package pl.ks.jfr.parser;

import lombok.Builder;
import lombok.Value;
import pl.ks.jfr.parser.tuning.AdditionalLevel;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Value
@Builder
public class JfrParsedLockEvent implements JfrParsedCommonStackTraceEvent {
    String[] stackTrace;
    int[] lineNumbers;
    long correlationId;
    long duration;
    String threadName;
    String filename;
    Instant eventTime;
    String monitorClass;

    public List<String[]> getFullStackTrace(Set<AdditionalLevel> additionalLevels) {
        List<String[]> fullStackTrace = new ArrayList<>();
        addCommonStackTraceElements(fullStackTrace, additionalLevels);
        fullStackTrace.add(new String[]{monitorClass +  "_[i]"});
        return fullStackTrace;
    }
}
