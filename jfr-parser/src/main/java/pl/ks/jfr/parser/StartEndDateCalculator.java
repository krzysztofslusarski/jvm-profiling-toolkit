package pl.ks.jfr.parser;

import lombok.Getter;

import java.time.Instant;

@Getter
class StartEndDateCalculator {
    private Instant startDate;
    private Instant endDate;

    synchronized void newDate(Instant instant) {
        if (startDate == null || startDate.isAfter(instant)) {
            startDate = instant;
        }
        if (endDate == null || endDate.isBefore(instant)) {
            endDate = instant;
        }
    }
}
