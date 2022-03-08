package pl.ks.jvm.logs.model;

import java.math.BigDecimal;
import lombok.Value;

@Value(staticConstructor = "of")
public class Time {
    private static final BigDecimal MILLI_TO_SECONDS = new BigDecimal("1000");
    private static final BigDecimal NANO_TO_MILLI = new BigDecimal("1000000");

    BigDecimal value;
    Unit unit;

    public Time toSeconds() {
        BigDecimal newValue = null;
        switch (unit) {
            case SECONDS -> {
                return this;
            }
            case MILLI_SECONDS -> {
                newValue = value.multiply(MILLI_TO_SECONDS);
            }
            case NANO_SECONDS -> {
                newValue = value.multiply(MILLI_TO_SECONDS).multiply(NANO_TO_MILLI);
            }
        }
        if (newValue == null) {
            throw new IllegalStateException();
        }
        return of(newValue, Unit.SECONDS);
    }

    public enum Unit {
        SECONDS,
        MILLI_SECONDS,
        NANO_SECONDS,
    }
}
