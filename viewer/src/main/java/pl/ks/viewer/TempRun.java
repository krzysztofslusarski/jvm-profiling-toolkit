package pl.ks.viewer;

import static org.springframework.http.MediaType.TEXT_HTML_VALUE;

import java.nio.file.Path;
import java.util.Collections;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import pl.ks.collapsed.CollapsedStack;
import pl.ks.jfr.parser.JfrParsedExecutionSampleEvent;
import pl.ks.jfr.parser.JfrParsedFile;
import pl.ks.jfr.parser.JfrParser;
import pl.ks.viewer.flamegraph.FlameGraphExecutor;

@Controller
@RequiredArgsConstructor
class TempRun {
    private final JfrParser jfrParser;
    private final FlameGraphExecutor flameGraphExecutor;

    @RequestMapping(value = "/try", produces = TEXT_HTML_VALUE)
    @ResponseBody
    public String run() {
        JfrParsedFile parsedFile = jfrParser.parse(Collections.singletonList(Path.of("/home/pasq/Hazelcast/ContextProfiling/10.212.1.101.jfr.gz")));
        CollapsedStack collapsedStack = parsedFile.asCollapsed(parsedFile.getExecutionSamples(), JfrParsedExecutionSampleEvent::getStackTrace);
        byte[] bytes = flameGraphExecutor.generateFlameGraphHtml5(collapsedStack, "title", false);
        return new String(bytes);
    }
}
