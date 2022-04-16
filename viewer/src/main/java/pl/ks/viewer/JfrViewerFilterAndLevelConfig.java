package pl.ks.viewer;

import java.util.Set;
import lombok.Builder;
import lombok.Value;
import pl.ks.jfr.parser.tuning.AdditionalLevel;

@Value
@Builder
public class JfrViewerFilterAndLevelConfig {
    boolean threadFilterOn;
    String threadFilter;
    boolean endDurationOn;
    String endDate;
    String endDateDateTimeFormat;
    long duration;
    boolean warmupCooldownOn;
    int cooldown;
    int warmup;
    boolean warmupDurationOn;
    int wdWarmup;
    long wdDuration;
    boolean ecidFilterOn;
    String ecidFilter;
    boolean startEndTimestampOn;
    long startTs;
    long endTs;
    Set<AdditionalLevel> additionalLevels;
}
