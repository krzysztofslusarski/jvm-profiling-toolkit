package pl.ks.viewer;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import lombok.RequiredArgsConstructor;
import pl.ks.collapsed.CollapsedStack;
import pl.ks.jfr.parser.JfrParsedAllocationEvent;
import pl.ks.jfr.parser.JfrParsedExecutionSampleEvent;
import pl.ks.jfr.parser.JfrParsedFile;
import pl.ks.jfr.parser.JfrParser;
import pl.ks.viewer.flamegraph.FlameGraphExecutor;

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

    JfrParsedFile getFile(UUID uuid) {
        return parsedFiles.get(uuid);
    }

    byte[] getExecutionSamples(UUID uuid, JfrViewerFilterAndLevelConfig config) {
        JfrParsedFile jfrParsedFile = parsedFiles.get(uuid);
        CollapsedStack collapsed = jfrParsedFile.asCollapsed(jfrParsedFile.getExecutionSamples(), config.getAdditionalLevels(), JfrParsedExecutionSampleEvent::getFullStackTrace);
        return flameGraphExecutor.generateFlameGraphHtml5(collapsed, "Execution samples", false);
    }

    byte[] getAllocationSamplesCount(UUID uuid, JfrViewerFilterAndLevelConfig config) {
        JfrParsedFile jfrParsedFile = parsedFiles.get(uuid);
        CollapsedStack collapsed = jfrParsedFile.asCollapsed(jfrParsedFile.getAllocationSamples(), config.getAdditionalLevels(), JfrParsedAllocationEvent::getFullStackTrace);
        return flameGraphExecutor.generateFlameGraphHtml5(collapsed, "Allocation samples (count)", false);
    }

    byte[] getAllocationSamplesSize(UUID uuid, JfrViewerFilterAndLevelConfig config) {
        JfrParsedFile jfrParsedFile = parsedFiles.get(uuid);
        CollapsedStack collapsed = jfrParsedFile.asCollapsed(jfrParsedFile.getAllocationSamples(), config.getAdditionalLevels(), JfrParsedAllocationEvent::getFullStackTrace, JfrParsedAllocationEvent::getSize);
        return flameGraphExecutor.generateFlameGraphHtml5(collapsed, "Allocation samples (size)", false);
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
}
