package pl.ks.jfr.parser;

import java.time.Instant;

public interface JfrParsedEventWithTime {
    Instant getEventTime();
}
