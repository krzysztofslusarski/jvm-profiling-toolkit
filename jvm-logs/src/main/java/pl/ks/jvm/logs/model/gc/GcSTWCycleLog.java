package pl.ks.jvm.logs.model.gc;

import lombok.Builder;
import lombok.Value;
import pl.ks.jvm.logs.model.Size;

@Value
@Builder
public class GcSTWCycleLog {
    Size heapBeforeGC;
    Size heapAfterGC;
    Size heapSize;
}
