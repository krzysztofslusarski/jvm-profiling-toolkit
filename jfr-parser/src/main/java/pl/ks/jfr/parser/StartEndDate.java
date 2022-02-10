package pl.ks.jfr.parser;

import java.time.Instant;
import lombok.Builder;
import lombok.Value;

@Builder
@Value
public class StartEndDate {
    Instant startDate;
    Instant endDate;
}
