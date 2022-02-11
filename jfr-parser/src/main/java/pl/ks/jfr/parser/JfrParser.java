package pl.ks.jfr.parser;

import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;
import pl.ks.jfr.parser.filter.PreStackFilter;

public interface JfrParser {
    JfrParsedFile parse(List<Path> jfrFiles, List<PreStackFilter> filters);

    StartEndDate calculateDatesWithCoolDownAndWarmUp(Stream<Path> paths, int warmUp, int coolDown);
}
