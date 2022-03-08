package pl.ks.jvm.logs.model.gc;

import java.util.List;
import lombok.Builder;
import lombok.Value;
import pl.ks.jvm.logs.model.JvmLogEntry;

@Value
@Builder
public class GcLog implements JvmLogEntry {
    List<GcSTWCycleLog> stwCycles;
}
