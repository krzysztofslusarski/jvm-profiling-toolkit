package pl.ks.jvm.logs.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import lombok.Value;

@Value(staticConstructor = "of")
public class Size {
    private static final BigDecimal UNIT_CHANGE = new BigDecimal("1024");

    BigDecimal value;
    Unit unit;

    public Size toMegaBytes() {
        BigDecimal newValue = null;
        switch (unit) {
            case MB -> {
                return this;
            }
            case B -> {
                newValue = value.multiply(UNIT_CHANGE).multiply(UNIT_CHANGE);
            }
            case KB -> {
                newValue = value.multiply(UNIT_CHANGE);
            }
            case GB -> {
                newValue = value.divide(UNIT_CHANGE,10, RoundingMode.HALF_EVEN);
            }
        }
        if (newValue == null) {
            throw new IllegalStateException();
        }
        return of(newValue, Unit.MB);
    }

    public enum Unit {
        B, KB, MB, GB
    }
}
