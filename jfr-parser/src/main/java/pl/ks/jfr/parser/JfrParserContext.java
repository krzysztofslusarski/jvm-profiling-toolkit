package pl.ks.jfr.parser;

import java.nio.file.Path;
import java.util.List;
import lombok.Builder;
import lombok.Value;
import pl.ks.jfr.parser.filter.PreStackFilter;

@Value
@Builder
class JfrParserContext {
    List<PreStackFilter> preStackFilters;
    JfrParsedFile jfrParsedFile;
    Path file;

    boolean includeThreadName = true;
    boolean includeFileName = true;
    boolean includeTimeStampAndDate = true;
    long timeStampDivider = 1000;
}
