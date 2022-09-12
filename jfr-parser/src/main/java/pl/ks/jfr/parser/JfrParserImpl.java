package pl.ks.jfr.parser;

import java.nio.file.Path;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StopWatch;

@Slf4j
class JfrParserImpl implements JfrParser {
    @Override
    public JfrParsedFile parse(List<Path> jfrFiles) {
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();



        stopWatch.stop();
        log.info("Parsing took: {}ms", stopWatch.getLastTaskTimeMillis());

        return null;
    }
}
