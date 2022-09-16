package pl.ks.viewer;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import lombok.RequiredArgsConstructor;
import pl.ks.jfr.parser.JfrParsedFile;
import pl.ks.jfr.parser.JfrParser;

@RequiredArgsConstructor
class StatefulJfrViewerService {
    private final Map<UUID, JfrParsedFile> parsedFiles = new ConcurrentHashMap<>();

    private final JfrParser jfrParser;

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
