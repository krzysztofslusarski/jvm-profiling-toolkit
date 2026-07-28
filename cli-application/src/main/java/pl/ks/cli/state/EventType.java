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

import java.util.UUID;
import java.util.function.Predicate;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import pl.ks.collapsed.CollapsedStack;
import pl.ks.jfr.parser.JfrParsedFile;
import pl.ks.viewer.JfrViewerFilterAndLevelConfig;
import pl.ks.viewer.StatefulJfrViewerService;
import pl.ks.viewer.TimeTable;

/**
 * Event types that can be rendered, mirroring the "Events" section of the web viewer. Every constant knows how to ask
 * {@link StatefulJfrViewerService} for its data, so pages do not have to switch over the type.
 */
@Getter
@RequiredArgsConstructor
public enum EventType {
    EXECUTION_SAMPLES("Execution samples", "Samples",
            file -> !file.getExecutionSamples().isEmpty(),
            StatefulJfrViewerService::getExecutionSamplesCollapsed,
            StatefulJfrViewerService::getExecutionSamplesTimeStats,
            StatefulJfrViewerService::getExecutionSamplesFlameGraph),
    WALL_CLOCK_SAMPLES("Wall-clock samples", "Samples",
            file -> !file.getWallClockSamples().isEmpty(),
            StatefulJfrViewerService::getWallClockSamplesCollapsed,
            StatefulJfrViewerService::getWallClockSamplesTimeStats,
            StatefulJfrViewerService::getWallClockSamplesFlameGraph),
    ALLOCATION_COUNT("Alloc. samples (count)", "Samples",
            file -> !file.getAllocationSamples().isEmpty(),
            StatefulJfrViewerService::getAllocationCountSamplesCollapsed,
            StatefulJfrViewerService::getAllocationCountSamplesTimeStats,
            StatefulJfrViewerService::getAllocationCountSamplesFlameGraph),
    ALLOCATION_SIZE("Alloc. samples (size)", "Bytes",
            file -> !file.getAllocationSamples().isEmpty(),
            StatefulJfrViewerService::getAllocationSizeSamplesCollapsed,
            StatefulJfrViewerService::getAllocationSizeSamplesTimeStats,
            StatefulJfrViewerService::getAllocationSizeSamplesFlameGraph),
    LOCK_COUNT("Lock samples (count)", "Samples",
            file -> !file.getLockSamples().isEmpty(),
            StatefulJfrViewerService::getLockCountSamplesCollapsed,
            StatefulJfrViewerService::getLockCountSamplesTimeStats,
            StatefulJfrViewerService::getLockCountSamplesFlameGraph),
    LOCK_TIME("Lock samples (time)", "Nanos",
            file -> !file.getLockSamples().isEmpty(),
            StatefulJfrViewerService::getLockTimeSamplesCollapsed,
            StatefulJfrViewerService::getLockTimeSamplesTimeStats,
            StatefulJfrViewerService::getLockTimeSamplesFlameGraph),
    ;

    private final String title;
    private final String unit;
    private final Predicate<JfrParsedFile> availability;
    private final CollapsedAccessor collapsedAccessor;
    private final TimeStatsAccessor timeStatsAccessor;
    private final FlameGraphHtmlAccessor flameGraphHtmlAccessor;

    public boolean isAvailableIn(JfrParsedFile file) {
        return availability.test(file);
    }

    public CollapsedStack collapsed(StatefulJfrViewerService service, UUID fileId, JfrViewerFilterAndLevelConfig config) {
        return collapsedAccessor.get(service, fileId, config);
    }

    public TimeTable timeStats(StatefulJfrViewerService service, UUID fileId, JfrViewerFilterAndLevelConfig config, TimeTable.Type type) {
        return timeStatsAccessor.get(service, fileId, config, type);
    }

    public byte[] flameGraphHtml(StatefulJfrViewerService service, UUID fileId, JfrViewerFilterAndLevelConfig config) {
        return flameGraphHtmlAccessor.get(service, fileId, config);
    }

    @FunctionalInterface
    interface CollapsedAccessor {
        CollapsedStack get(StatefulJfrViewerService service, UUID fileId, JfrViewerFilterAndLevelConfig config);
    }

    @FunctionalInterface
    interface TimeStatsAccessor {
        TimeTable get(StatefulJfrViewerService service, UUID fileId, JfrViewerFilterAndLevelConfig config, TimeTable.Type type);
    }

    @FunctionalInterface
    interface FlameGraphHtmlAccessor {
        byte[] get(StatefulJfrViewerService service, UUID fileId, JfrViewerFilterAndLevelConfig config);
    }
}
