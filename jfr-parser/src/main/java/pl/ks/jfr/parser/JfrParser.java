package pl.ks.jfr.parser;

import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;
import pl.ks.jfr.parser.tuning.AdditionalLevel;
import pl.ks.jfr.parser.tuning.PreStackFilter;

public interface JfrParser {
    JfrParsedFile parse(List<Path> jfrFiles, List<PreStackFilter> filters, Set<AdditionalLevel> additionalLevels, boolean ecidIsUuid);

    StartEndDate calculateDatesWithCoolDownAndWarmUp(Stream<Path> paths, int warmUp, int coolDown);
}
