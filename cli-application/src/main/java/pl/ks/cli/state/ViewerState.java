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
package pl.ks.cli.state;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import lombok.Getter;
import lombok.Setter;
import pl.ks.jfr.parser.tuning.AdditionalLevel;
import pl.ks.viewer.JfrViewerFilterAndLevelConfig;

/**
 * Everything the user can tune from the console, i.e. the equivalent of the whole sidebar of the web viewer. Dialogs
 * mutate this object and {@link #toConfig()} turns it into the config understood by the shared viewer service.
 */
@Getter
@Setter
public class ViewerState {
    private PageType page = PageType.FLAME_GRAPH;
    private EventType eventType;

    // options
    private int tableLimit = 10_000;
    private boolean reverseOn;

    // additional levels
    private Set<AdditionalLevel> additionalLevels = EnumSet.noneOf(AdditionalLevel.class);

    // filters
    private boolean threadFilterOn;
    private String threadFilter = "";
    private boolean threadFilterContainsOn;
    private String threadFilterContains = "";
    private boolean stackTraceFilterOn;
    private List<String> stackTraceFilters = new ArrayList<>();
    private boolean stackTraceNotContainsFilterOn;
    private List<String> stackTraceNotContainsFilters = new ArrayList<>();
    private boolean spanFilterEqualsOn;
    private String spanFilterEquals = "";
    private boolean spanFilterContainsOn;
    private List<String> spanFilterContains = new ArrayList<>();
    private boolean endDurationOn;
    private String endDate = "";
    private String endDateDateTimeFormat = "dd/MMM/yyyy:HH:mm:ss Z";
    private long duration;
    private String localeLanguage = "EN";
    private boolean startEndTimestampOn;
    private long startTs;
    private long endTs;
    private boolean warmupCooldownOn;
    private int warmup;
    private int cooldown;
    private boolean warmupDurationOn;
    private int wdWarmup;
    private long wdDuration;
    private boolean consumeCpuOn;

    public JfrViewerFilterAndLevelConfig toConfig() {
        return JfrViewerFilterAndLevelConfig.builder()
                .consumeCpuOn(consumeCpuOn)
                .reverseOn(reverseOn)
                .threadFilterOn(threadFilterOn)
                .threadFilter(threadFilter)
                .threadFilterContainsOn(threadFilterContainsOn)
                .threadFilterContains(threadFilterContains)
                .stackTraceFilterOn(stackTraceFilterOn)
                .stackTraceFilters(List.copyOf(stackTraceFilters))
                .stackTraceNotContainsFilterOn(stackTraceNotContainsFilterOn)
                .stackTraceNotContainsFilters(List.copyOf(stackTraceNotContainsFilters))
                .spanFilterEqualsOn(spanFilterEqualsOn)
                .spanFilterEquals(spanFilterEquals)
                .spanFilterContainsOn(spanFilterContainsOn)
                .spanFilterContains(List.copyOf(spanFilterContains))
                .endDurationOn(endDurationOn)
                .endDate(endDate)
                .endDateDateTimeFormat(endDateDateTimeFormat)
                .duration(duration)
                .localeLanguage(localeLanguage)
                .startEndTimestampOn(startEndTimestampOn)
                .startTs(startTs)
                .endTs(endTs)
                .warmupCooldownOn(warmupCooldownOn)
                .warmup(warmup)
                .cooldown(cooldown)
                .warmupDurationOn(warmupDurationOn)
                .wdWarmup(wdWarmup)
                .wdDuration(wdDuration)
                .additionalLevels(EnumSet.copyOf(additionalLevels))
                .tableLimit(tableLimit)
                .build();
    }

    public String describeFilters() {
        List<String> active = new ArrayList<>();
        if (threadFilterOn) {
            active.add("thread=" + threadFilter);
        }
        if (threadFilterContainsOn) {
            active.add("thread~" + threadFilterContains);
        }
        if (stackTraceFilterOn && !stackTraceFilters.isEmpty()) {
            active.add("stack~" + String.join("+", stackTraceFilters));
        }
        if (stackTraceNotContainsFilterOn && !stackTraceNotContainsFilters.isEmpty()) {
            active.add("stack!~" + String.join("+", stackTraceNotContainsFilters));
        }
        if (spanFilterEqualsOn) {
            active.add("span=" + spanFilterEquals);
        }
        if (spanFilterContainsOn && !spanFilterContains.isEmpty()) {
            active.add("span~" + String.join("+", spanFilterContains));
        }
        if (endDurationOn) {
            active.add("access log " + endDate + " -" + duration + "ms");
        }
        if (startEndTimestampOn) {
            active.add("ts " + startTs + ".." + endTs);
        }
        if (warmupCooldownOn) {
            active.add("warmup " + warmup + "s / cooldown " + cooldown + "s");
        }
        if (warmupDurationOn) {
            active.add("warmup " + wdWarmup + "s / duration " + wdDuration + "s");
        }
        if (consumeCpuOn) {
            active.add("consumes CPU");
        }
        return active.isEmpty() ? "none" : String.join(", ", active);
    }

    public String describeLevels() {
        if (additionalLevels.isEmpty()) {
            return "none";
        }
        List<String> names = new ArrayList<>(additionalLevels.size());
        for (AdditionalLevel level : additionalLevels) {
            names.add(level.name().toLowerCase().replace('_', ' '));
        }
        return String.join(", ", names);
    }
}
