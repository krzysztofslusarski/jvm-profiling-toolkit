package pl.ks.viewer;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import pl.ks.collapsed.CollapsedStack;
import pl.ks.jfr.parser.JfrEcidInfo;
import pl.ks.jfr.parser.JfrParsedAllocationEvent;
import pl.ks.jfr.parser.JfrParsedCommonStackTraceEvent;
import pl.ks.jfr.parser.JfrParsedCpuUsageEvent;
import pl.ks.jfr.parser.JfrParsedEventWithTime;
import pl.ks.jfr.parser.JfrParsedExecutionSampleEvent;
import pl.ks.jfr.parser.JfrParsedFile;
import pl.ks.jfr.parser.JfrParsedLockEvent;
import pl.ks.jfr.parser.JfrParser;
import pl.ks.viewer.flamegraph.FlameGraphExecutor;

@Slf4j
@RequiredArgsConstructor
class StatefulJfrViewerService {
    private final Map<UUID, JfrParsedFile> parsedFiles = new ConcurrentHashMap<>();

    private final JfrParser jfrParser;
    private final FlameGraphExecutor flameGraphExecutor;

    List<StatefulJfrFile> getFiles() {
        return parsedFiles.entrySet().stream()
                .sorted(Comparator.comparing(o -> o.getValue().getParseStartDate()))
                .map(entry -> StatefulJfrFile.builder()
                        .id(entry.getKey())
                        .parseStartDate(entry.getValue().getParseStartDate())
                        .filenames(entry.getValue().getFilenames())
                        .build()
                )
                .toList();
    }

    void remove(UUID uuid) {
        parsedFiles.remove(uuid);
    }

    JfrParsedFile getFile(UUID uuid) {
        return parsedFiles.get(uuid);
    }

    StatefulJfrHighChartCpuStats getCpuStats(UUID uuid, JfrViewerFilterAndLevelConfig config) {
        JfrParsedFile jfrParsedFile = parsedFiles.get(uuid);
        List<Predicate<JfrParsedCpuUsageEvent>> filters = createFilters(config, jfrParsedFile, JfrParsedCpuUsageEvent.class);

        Stream<JfrParsedCpuUsageEvent> samples = jfrParsedFile.getCpuUsageSamples().stream();
        for (Predicate<JfrParsedCpuUsageEvent> filter : filters) {
            samples = samples.filter(filter);
        }
        List<JfrParsedCpuUsageEvent> cpuUsageEvents = samples.sorted(Comparator.comparing(JfrParsedCpuUsageEvent::getEventTime)).toList();
        Map<String, List<JfrParsedCpuUsageEvent>> cpuUsageEventsPerFile = new HashMap<>();
        for (JfrParsedCpuUsageEvent event : cpuUsageEvents) {
            cpuUsageEventsPerFile.computeIfAbsent(event.getFilename(), name -> new ArrayList<>()).add(event);
        }

        return StatefulJfrHighChartCpuStats.builder()
                .filenames(jfrParsedFile.getFilenames())
                .cpuUsageEventsPerFile(cpuUsageEventsPerFile)
                .build();
    }

    List<JfrEcidInfo> getCorrelationIdStats(UUID uuid, JfrViewerFilterAndLevelConfig config) {
        Map<Long, JfrEcidInfo> correlationIdInfo = new ConcurrentHashMap<>();
        getFilteredExecutionSamples(config, getFile(uuid)).stream().parallel().forEach(sample -> {
            correlationIdInfo.computeIfAbsent(sample.getCorrelationId(), JfrEcidInfo::new)
                    .newExecutionSample(sample.getEventTime(), sample.isConsumesCpu());
        });
        return correlationIdInfo.values().stream()
                .sorted(Comparator.comparing(JfrEcidInfo::timeDiff).reversed())
                .limit(1000)
                .toList();
    }

    byte[] getExecutionSamplesFlameGraph(UUID uuid, JfrViewerFilterAndLevelConfig config) {
        JfrParsedFile jfrParsedFile = parsedFiles.get(uuid);
        CollapsedStack collapsed = jfrParsedFile.asCollapsed(getFilteredExecutionSamples(config, jfrParsedFile),
                config.getAdditionalLevels(), JfrParsedExecutionSampleEvent::getFullStackTrace);
        return flameGraphExecutor.generateFlameGraphHtml5(collapsed, "Execution samples", config.isReverseOn());
    }

    SelfAndTotalTimeStats getExecutionSamplesTimeStats(UUID uuid, JfrViewerFilterAndLevelConfig config) {
        JfrParsedFile jfrParsedFile = parsedFiles.get(uuid);
        return generateTimeStats(getFilteredExecutionSamples(config, jfrParsedFile));
    }

    private List<JfrParsedExecutionSampleEvent> getFilteredExecutionSamples(JfrViewerFilterAndLevelConfig config, JfrParsedFile jfrParsedFile) {
        List<Predicate<JfrParsedExecutionSampleEvent>> filters = createFilters(config, jfrParsedFile, JfrParsedExecutionSampleEvent.class);

        Stream<JfrParsedExecutionSampleEvent> samples = jfrParsedFile.getExecutionSamples().stream();
        for (Predicate<JfrParsedExecutionSampleEvent> filter : filters) {
            samples = samples.filter(filter);
        }
        return samples.toList();
    }

    byte[] getAllocationSamplesCountFlameGraph(UUID uuid, JfrViewerFilterAndLevelConfig config) {
        JfrParsedFile jfrParsedFile = parsedFiles.get(uuid);
        List<Predicate<JfrParsedAllocationEvent>> filters = createFilters(config, jfrParsedFile, JfrParsedAllocationEvent.class);

        Stream<JfrParsedAllocationEvent> samples = jfrParsedFile.getAllocationSamples().stream();
        for (Predicate<JfrParsedAllocationEvent> filter : filters) {
            samples = samples.filter(filter);
        }
        CollapsedStack collapsed = jfrParsedFile.asCollapsed(samples.toList(), config.getAdditionalLevels(), JfrParsedAllocationEvent::getFullStackTrace);
        return flameGraphExecutor.generateFlameGraphHtml5(collapsed, "Allocation samples (count)", config.isReverseOn());
    }

    byte[] getAllocationSamplesSizeFlameGraph(UUID uuid, JfrViewerFilterAndLevelConfig config) {
        JfrParsedFile jfrParsedFile = parsedFiles.get(uuid);
        List<Predicate<JfrParsedAllocationEvent>> filters = createFilters(config, jfrParsedFile, JfrParsedAllocationEvent.class);

        Stream<JfrParsedAllocationEvent> samples = jfrParsedFile.getAllocationSamples().stream();
        for (Predicate<JfrParsedAllocationEvent> filter : filters) {
            samples = samples.filter(filter);
        }
        CollapsedStack collapsed = jfrParsedFile.asCollapsed(samples.toList(), config.getAdditionalLevels(), JfrParsedAllocationEvent::getFullStackTrace, JfrParsedAllocationEvent::getSize);
        return flameGraphExecutor.generateFlameGraphHtml5(collapsed, "Allocation samples (size)", config.isReverseOn());
    }

    byte[] getLockSamplesFlameGraph(UUID uuid, JfrViewerFilterAndLevelConfig config) {
        JfrParsedFile jfrParsedFile = parsedFiles.get(uuid);
        List<Predicate<JfrParsedLockEvent>> filters = createFilters(config, jfrParsedFile, JfrParsedLockEvent.class);

        Stream<JfrParsedLockEvent> samples = jfrParsedFile.getLockSamples().stream();
        for (Predicate<JfrParsedLockEvent> filter : filters) {
            samples = samples.filter(filter);
        }
        CollapsedStack collapsed = jfrParsedFile.asCollapsed(samples.toList(), config.getAdditionalLevels(), JfrParsedLockEvent::getFullStackTrace);
        return flameGraphExecutor.generateFlameGraphHtml5(collapsed, "Lock samples", config.isReverseOn());
    }

    UUID parseNewFiles(List<String> files) {
        UUID uuid = UUID.randomUUID();
        List<Path> paths = files.stream()
                .map(Paths::get)
                .toList();

        JfrParsedFile parsedFile = jfrParser.parse(paths);
        parsedFiles.put(uuid, parsedFile);
        return uuid;
    }

    private SelfAndTotalTimeStats generateTimeStats(List<? extends JfrParsedCommonStackTraceEvent> events) {
        SelfAndTotalTimeStats selfAndTotalTimeStats = new SelfAndTotalTimeStats();
        events.stream().parallel()
                .forEach(event -> {
                    String[] stackTrace = event.getStackTrace();
                    Set<String> visited = new HashSet<>();
                    selfAndTotalTimeStats.newStackTrace();
                    for (int i = stackTrace.length - 1; i >= 0; i--) {
                        if (visited.contains(stackTrace[i])) {
                            continue;
                        }
                        visited.add(stackTrace[i]);
                        selfAndTotalTimeStats.methodSample(stackTrace[i], i == stackTrace.length - 1);
                    }
                });
        return selfAndTotalTimeStats;
    }

    @SneakyThrows
    private <T> List<Predicate<T>> createFilters(JfrViewerFilterAndLevelConfig config, JfrParsedFile jfrParsedFile, Class<T> clazz) {
        List<Predicate<T>> filters = new ArrayList<>(2);

        if (JfrParsedExecutionSampleEvent.class.isAssignableFrom(clazz)) {
            if (config.isConsumeCpuOn()) {
                filters.add(t -> ((JfrParsedExecutionSampleEvent) t).isConsumesCpu());
            }
        }

        if (JfrParsedCommonStackTraceEvent.class.isAssignableFrom(clazz)) {
            if (config.isThreadFilterOn()) {
                filters.add(t -> ((JfrParsedCommonStackTraceEvent) t).getThreadName().equalsIgnoreCase(config.getThreadFilter()));
            }
            if (config.isEcidFilterOn()) {
                filters.add(t -> ((JfrParsedCommonStackTraceEvent) t).getCorrelationId() == config.getEcidFilter());
            }
        }

        if (JfrParsedEventWithTime.class.isAssignableFrom(clazz)) {
            if (config.isEndDurationOn()) {
                SimpleDateFormat simpleDateFormat = new SimpleDateFormat(config.getEndDateDateTimeFormat());
                Date parsedDate = simpleDateFormat.parse(config.getEndDate());
                Instant endDate = parsedDate.toInstant();
                Instant startDate = endDate.minus(config.getDuration(), ChronoUnit.MILLIS);
                filters.add(t -> {
                    JfrParsedEventWithTime event = (JfrParsedEventWithTime) t;
                    return !(event.getEventTime().isBefore(startDate) || event.getEventTime().isAfter(endDate));
                });
            } else if (config.isWarmupCooldownOn()) {
                Instant startDate = jfrParsedFile.getMinEventDate().plus(config.getWarmup(), ChronoUnit.SECONDS);
                Instant endDate = jfrParsedFile.getMaxEventDate().minus(config.getCooldown(), ChronoUnit.SECONDS);
                filters.add(t -> {
                    JfrParsedEventWithTime event = (JfrParsedEventWithTime) t;
                    return !(event.getEventTime().isBefore(startDate) || event.getEventTime().isAfter(endDate));
                });
            } else if (config.isWarmupDurationOn()) {
                Instant startDate = jfrParsedFile.getMinEventDate().plus(config.getWdWarmup(), ChronoUnit.SECONDS);
                Instant endDate = startDate.plus(config.getWdDuration(), ChronoUnit.SECONDS);
                filters.add(t -> {
                    JfrParsedEventWithTime event = (JfrParsedEventWithTime) t;
                    return !(event.getEventTime().isBefore(startDate) || event.getEventTime().isAfter(endDate));
                });
            } else if (config.isStartEndTimestampOn()) {
                filters.add(t -> {
                    JfrParsedEventWithTime event = (JfrParsedEventWithTime) t;
                    long startTs = config.getStartTs() * 1000;
                    long endTs = (config.getEndTs() * 1000) + 999;
                    return !(event.getEventTime().toEpochMilli() < startTs || event.getEventTime().toEpochMilli() > endTs);
                });
            }
        }

        return filters;
    }
}
