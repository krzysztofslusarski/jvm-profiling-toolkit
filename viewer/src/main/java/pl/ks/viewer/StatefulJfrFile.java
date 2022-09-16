package pl.ks.viewer;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class StatefulJfrFile {
    UUID id;
    Instant parseStartDate;
    List<String> filenames;
}
