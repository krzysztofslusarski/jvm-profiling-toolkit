package pl.ks.viewer;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import pl.ks.collapsed.CollapsedStack;
import pl.ks.collapsed.CollapsedStackWriter;
import pl.ks.jfr.parser.JfrParsedFile;
import pl.ks.jfr.parser.JfrParser;
import pl.ks.jfr.parser.StartEndDate;
import pl.ks.jfr.parser.filter.PreStackFilter;
import pl.ks.jfr.parser.filter.StartEndDateFilter;
import pl.ks.jfr.parser.filter.ThreadNameFilter;
import pl.ks.viewer.io.TempFileUtils;

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
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@RequiredArgsConstructor
class JfrViewerService {
    private final JfrParser jfrParser;

    JfrViewerResult convertToCollapsed(List<String> files, JfrViewerFilterConfig config) throws IOException {
        List<Path> paths = files.stream()
                .map(Paths::get)
                .collect(Collectors.toList());

        JfrParsedFile parsedFile = jfrParser.parse(paths, createFilters(config, paths));
        Map<JfrParsedFile.Type, String> collapsedFiles = new LinkedHashMap<>();
        for (JfrParsedFile.Type type : JfrParsedFile.Type.values()) {
            CollapsedStack collapsedStack = parsedFile.get(type);
            if (collapsedStack.isNotEmpty()) {
                String fileName = "collapsed-" + UUID.randomUUID() + ".log";
                CollapsedStackWriter.saveFile(TempFileUtils.TEMP_DIR, fileName, collapsedStack);
                collapsedFiles.put(type, fileName);
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
                .jfrParsedFile(parsedFile)
                .build();
    }

    @SneakyThrows
    private List<PreStackFilter> createFilters(JfrViewerFilterConfig config, List<Path> paths) {
        List<PreStackFilter> filters = new ArrayList<>(2);

        if (config.isThreadFilterOn()) {
            filters.add(
                    ThreadNameFilter.builder()
                            .threadName(config.getThreadFilter())
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
            StartEndDate startEndDate = jfrParser.calculateDatesWithCoolDownAndWarmUp(paths.stream(), config.getWarmup(), config.getCooldown());
            filters.add(
                    StartEndDateFilter.builder()
                            .startDate(startEndDate.getStartDate())
                            .endDate(startEndDate.getEndDate())
                            .build()
            );
        }

        return filters;
    }
}
