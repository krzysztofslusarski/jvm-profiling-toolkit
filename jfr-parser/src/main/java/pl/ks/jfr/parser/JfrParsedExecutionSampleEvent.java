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

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import lombok.Builder;
import lombok.Value;
import pl.ks.jfr.parser.tuning.AdditionalLevel;

@Value
@Builder
public class JfrParsedExecutionSampleEvent implements JfrParsedCommonStackTraceEvent {
    String[] stackTrace;
    int[] lineNumbers;
    long correlationId;
    String threadName;
    String filename;
    Instant eventTime;
    boolean consumesCpu;

    public List<String[]> getFullStackTrace(Set<AdditionalLevel> additionalLevels) {
        List<String[]> fullStackTrace = new ArrayList<>();
        addCommonStackTraceElements(fullStackTrace, additionalLevels);
        return fullStackTrace;
    }
}
