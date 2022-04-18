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

import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import lombok.Builder;
import lombok.Value;
import pl.ks.jfr.parser.tuning.AdditionalLevel;
import pl.ks.jfr.parser.tuning.PreStackFilter;

@Value
@Builder
class JfrParserContext {
    boolean ecidIsUuid;
    Path file;
    List<PreStackFilter> preStackFilters;
    JfrParsedFile jfrParsedFile;
    Set<AdditionalLevel> additionalLevels;

    public boolean includeThreadName() {
        return additionalLevels.contains(AdditionalLevel.THREAD);
    }

    public boolean includeFileName() {
        return additionalLevels.contains(AdditionalLevel.FILENAME);
    }

    public boolean includeTimestamp10SAndDate() {
        return additionalLevels.contains(AdditionalLevel.TIMESTAMP_10_S);
    }

    public boolean includeTimestamp1SAndDate() {
        return additionalLevels.contains(AdditionalLevel.TIMESTAMP_1_S);
    }

    public boolean includeTimestamp100MSAndDate() {
        return additionalLevels.contains(AdditionalLevel.TIMESTAMP_100_MS);
    }

    public boolean includeAnyTimestampAndDate() {
        return includeTimestamp10SAndDate() || includeTimestamp1SAndDate() || includeTimestamp100MSAndDate();
    }
}
