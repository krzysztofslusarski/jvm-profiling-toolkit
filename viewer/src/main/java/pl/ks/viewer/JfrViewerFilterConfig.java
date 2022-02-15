package pl.ks.viewer;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class JfrViewerFilterConfig {
    private Boolean threadFilterOn;
    private String threadFilter;
    private Boolean endDurationOn;
    private String endDate;
    private String endDateDateTimeFormat;
    private Long duration;
}
