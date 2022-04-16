package pl.ks.jfr.parser;

import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import lombok.Builder;
import lombok.Value;
import pl.ks.jfr.parser.tuning.AdditionalLevel;
import pl.ks.jfr.parser.tuning.PreStackFilter;

@Value
@Builder
class JfrParserContext {
    List<PreStackFilter> preStackFilters;
    JfrParsedFile jfrParsedFile;
    Path file;

    Set<AdditionalLevel> additionalLevels;

    public boolean isIncludeThreadName() {
        return additionalLevels.contains(AdditionalLevel.THREAD);
    }

    public boolean isIncludeFileName() {
        return additionalLevels.contains(AdditionalLevel.FILE_NAME);
    }

    public boolean isIncludeTimestampAndDate() {
        return additionalLevels.contains(AdditionalLevel.TIMESTAMP);
    }

    long timestampDivider = 1000;
}
