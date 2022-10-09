package pl.ks.viewer;

import lombok.Builder;
import lombok.Value;

import java.util.List;

@Value
@Builder
public class TimeTable {
    List<Row> rows;

    @Value
    @Builder
    public static class Row {
        String methodName;
        String percent;
        long samples;
    }

    public enum Type {
        TOTAL_TIME,
        SELF_TIME,
    }
}
