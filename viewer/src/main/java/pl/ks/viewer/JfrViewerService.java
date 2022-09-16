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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import pl.ks.collapsed.CollapsedStack;
import pl.ks.collapsed.CollapsedStackWriter;
import pl.ks.jfr.parser.JfrCollapsedParsedFile;
import pl.ks.jfr.parser.JfrCollapsedParser;
import pl.ks.jfr.parser.StartEndDate;
import pl.ks.jfr.parser.tuning.EcidFilter;
import pl.ks.jfr.parser.tuning.PreStackFilter;
import pl.ks.jfr.parser.tuning.StartEndDateFilter;
import pl.ks.jfr.parser.tuning.StartEndTimestampFilter;
import pl.ks.jfr.parser.tuning.ThreadNameFilter;
import pl.ks.viewer.io.TempFileUtils;

@Slf4j
@RequiredArgsConstructor
class JfrViewerService {
    private static final SimpleDateFormat OUTPUT_FORMAT = new SimpleDateFormat("dd/MMM/yyyy:HH:mm:ss Z", Locale.US);

    private final JfrCollapsedParser jfrCollapsedParser;

    JfrViewerResult convertToCollapsed(List<String> files, JfrViewerFilterAndLevelConfig config) throws IOException {
        List<Path> paths = files.stream()
                .map(Paths::get)
                .toList();

        JfrCollapsedParsedFile parsedFile = jfrCollapsedParser.parseCollapsed(paths, createFilters(config, paths), config.getAdditionalLevels());
        Map<JfrCollapsedParsedFile.Type, String> collapsedFiles = new LinkedHashMap<>();
        boolean ignoreWall = parsedFile.getCpu().hasSameSizes(parsedFile.getWall());

        for (JfrCollapsedParsedFile.Type type : JfrCollapsedParsedFile.Type.values()) {
            CollapsedStack collapsedStack = parsedFile.get(type);
            if (collapsedStack.isNotEmpty()) {
                if (ignoreWall && type == JfrCollapsedParsedFile.Type.WALL) {
                    continue;
                }
                String filename = "collapsed-" + UUID.randomUUID() + ".log";
                CollapsedStackWriter.saveFile(TempFileUtils.TEMP_DIR, filename, collapsedStack);
                collapsedFiles.put(type, filename);
            }
        }
        paths.forEach(path -> {
            try {
                Files.delete(path);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
        return JfrViewerResult.builder()
                .collapsedFiles(collapsedFiles)
                .jfrCollapsedParsedFile(parsedFile)
                .build();
    }

    @SneakyThrows
    private List<PreStackFilter> createFilters(JfrViewerFilterAndLevelConfig config, List<Path> paths) {
        List<PreStackFilter> filters = new ArrayList<>(2);

        if (config.isThreadFilterOn()) {
            filters.add(
                    ThreadNameFilter.builder()
                            .threadName(config.getThreadFilter())
                            .build()
            );
        }

        if (config.isEcidFilterOn()) {
            filters.add(
                    EcidFilter.builder()
                            .ecid(config.getEcidFilter())
                            .build()
            );
        }

        if (config.isEndDurationOn()) {
            SimpleDateFormat simpleDateFormat = new SimpleDateFormat(config.getEndDateDateTimeFormat());
            Date parsedDate = simpleDateFormat.parse(config.getEndDate());
            Instant endDate = parsedDate.toInstant();
            Instant startDate = endDate.minus(config.getDuration(), ChronoUnit.MILLIS);
            filters.add(
                    StartEndDateFilter.builder()
                            .startDate(startDate)
                            .endDate(endDate)
                            .build()
            );
        } else if (config.isWarmupCooldownOn()) {
            log.info("Warmup: {}, cooldown: {}", config.getWarmup(), config.getCooldown());
            StartEndDate startEndDate = jfrCollapsedParser.calculateDatesWithCoolDownAndWarmUp(paths.stream(), config.getWarmup(), config.getCooldown());

            log.info("Start date in access log format: {}", OUTPUT_FORMAT.format(new Date(startEndDate.getStartDate().toEpochMilli())));
            log.info("End date in access log format: {}", OUTPUT_FORMAT.format(new Date(startEndDate.getEndDate()   .toEpochMilli())));

            filters.add(
                    StartEndDateFilter.builder()
                            .startDate(startEndDate.getStartDate())
                            .endDate(startEndDate.getEndDate())
                            .build()
            );
        } else if (config.isWarmupDurationOn()) {
            log.info("Warmup: {}, duration: {}", config.getWdWarmup(), config.getWdDuration());
            StartEndDate startEndDate = jfrCollapsedParser.calculateDatesWithCoolDownAndWarmUp(paths.stream(), config.getWdWarmup(), 0);

            log.info("Start date in access log format: {}", OUTPUT_FORMAT.format(new Date(startEndDate.getStartDate().toEpochMilli())));

            filters.add(
                    StartEndDateFilter.builder()
                            .startDate(startEndDate.getStartDate())
                            .endDate(startEndDate.getStartDate().plus(config.getWdDuration(), ChronoUnit.SECONDS))
                            .build()
            );
        } else if (config.isStartEndTimestampOn()) {
            log.info("Start/end timestamp, start: {}, end: {}", config.getStartTs(), config.getEndTs());
            filters.add(
                    StartEndTimestampFilter.builder()
                            .startTs(config.getStartTs())
                            .endTs(config.getEndTs())
                            .build()
            );
        }

        return filters;
    }
}
