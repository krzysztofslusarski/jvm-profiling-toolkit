package pl.ks.viewer;

import java.util.List;
import java.util.UUID;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class TimeTable {
    UUID fileId;
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
