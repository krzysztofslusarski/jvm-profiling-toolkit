package pl.ks.jvm.logs.model.safepoint;

import lombok.Builder;
import lombok.Value;
import pl.ks.jvm.logs.model.Time;
import pl.ks.jvm.logs.model.Timestamp;

@Value
@Builder
public class SafepointOperationLog {
    Timestamp timeStamp;
    long sequenceId;
    String operationName;
    Time applicationTime;
    Time ttsTime;
    Time stoppedTime;
}
