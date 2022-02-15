package pl.ks.viewer;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class JfrViewerFilterConfig {
    boolean threadFilterOn;
    String threadFilter;
    boolean endDurationOn;
    String endDate;
    String endDateDateTimeFormat;
    long duration;
    boolean warmupCooldownOn;
    int cooldown;
    int warmup;
}
