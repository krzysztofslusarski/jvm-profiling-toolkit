package pl.ks.jfr.parser;

import lombok.Value;
import pl.ks.CollapsedStack;

@Value
public class JfrParsedFile {
    CollapsedStack wall = new CollapsedStack();
    CollapsedStack cpu = new CollapsedStack();
    CollapsedStack allocCount = new CollapsedStack();
    CollapsedStack allocSize = new CollapsedStack();
    CollapsedStack lock = new CollapsedStack();
}
