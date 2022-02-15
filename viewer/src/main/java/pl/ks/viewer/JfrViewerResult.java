package pl.ks.viewer;

import lombok.Builder;
import lombok.Value;
import pl.ks.jfr.parser.JfrParsedFile;

import java.util.Map;

@Value
@Builder
public class JfrViewerResult {
    Map<JfrParsedFile.Type, String> collapsedFiles;
    JfrParsedFile jfrParsedFile;
}
