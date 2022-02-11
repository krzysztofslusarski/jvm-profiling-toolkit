package pl.ks.viewer;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import pl.ks.collapsed.CollapsedStack;
import pl.ks.collapsed.CollapsedStackWriter;
import pl.ks.jfr.parser.JfrParsedFile;
import pl.ks.jfr.parser.JfrParser;
import pl.ks.viewer.io.TempFileUtils;

@RequiredArgsConstructor
class JfrViewerService {
    private final JfrParser jfrParser;

    Map<JfrParsedFile.Type, String> convertToCollapsed(List<String> files) throws IOException {
        List<Path> paths = files.stream()
                .map(Paths::get)
                .collect(Collectors.toList());

        JfrParsedFile parsedFile = jfrParser.parse(paths, Collections.emptyList());
        Map<JfrParsedFile.Type, String> collapsedFiles = new HashMap<>();
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
        return collapsedFiles;
    }
}
