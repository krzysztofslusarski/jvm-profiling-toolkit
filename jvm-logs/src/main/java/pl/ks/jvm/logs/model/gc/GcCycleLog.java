package pl.ks.jvm.logs.model.gc;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class GcCycleLog {
    int heapBeforeGCMb;
    int heapAfterGCMb;
    int heapSizeMb;
}
