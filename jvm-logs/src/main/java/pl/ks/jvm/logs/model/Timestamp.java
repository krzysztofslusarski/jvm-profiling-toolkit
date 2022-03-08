package pl.ks.jvm.logs.model;

import java.math.BigDecimal;
import lombok.Value;

@Value(staticConstructor = "of")
public class Timestamp {
    BigDecimal value;
}
