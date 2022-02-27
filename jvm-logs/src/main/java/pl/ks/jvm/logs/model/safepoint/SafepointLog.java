package pl.ks.jvm.logs.model.safepoint;

import java.util.List;
import lombok.Builder;
import lombok.Value;
import pl.ks.jvm.logs.model.JvmLogEntry;

@Value
@Builder
public class SafepointLog implements JvmLogEntry {
    List<SafepointOperationLog> safepoints;
}
