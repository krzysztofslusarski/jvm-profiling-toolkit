package pl.ks.jvm.logs.model.safepoint;

import java.math.BigDecimal;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class SafepointOperationLog {
    BigDecimal timeStamp;
    long sequenceId;
    String operationName;
    BigDecimal applicationTime;
    BigDecimal ttsTime;
    BigDecimal stoppedTime;
}
