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
package pl.ks.viewer;

import lombok.Builder;
import lombok.Value;
import pl.ks.jfr.parser.tuning.AdditionalLevel;

import java.util.Set;

@Value
@Builder
public class JfrViewerFilterAndLevelConfig {
    boolean threadFilterOn;
    String threadFilter;
    boolean endDurationOn;
    String endDate;
    String endDateDateTimeFormat;
    long duration;
    boolean warmupCooldownOn;
    int cooldown;
    int warmup;
    boolean warmupDurationOn;
    int wdWarmup;
    long wdDuration;
    boolean ecidFilterOn;
    long ecidFilter;
    boolean startEndTimestampOn;
    long startTs;
    long endTs;
    boolean consumeCpuOn;
    boolean reverseOn;
    Set<AdditionalLevel> additionalLevels;
    int tableLimit;
    String localeLanguage;
    boolean stackTraceFilterOn;
    String stackTraceFilter;
}
