package pl.ks.jvm.logs.parser.custom.safepoint;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import pl.ks.jvm.logs.model.safepoint.SafepointLog;
import pl.ks.jvm.logs.model.safepoint.SafepointOperationLog;
import pl.ks.jvm.logs.parser.JvmLogEntryProvider;

class SafepointParser implements JvmLogEntryProvider<SafepointLog> {
    @Getter
    private List<SafepointParserOperationLog> safepoints = new ArrayList<>();
    private SafepointParserOperationLog lastEntry;
    private long sequenceId;

    void newSafepoint(BigDecimal timeStamp) {
        if (lastEntry != null && lastEntry.isCompleted()) {
            safepoints.add(lastEntry);
        }
        lastEntry = new SafepointParserOperationLog(timeStamp, sequenceId++);
    }

    void newSafepoint(BigDecimal timeStamp, BigDecimal applicationTime) {
        if (lastEntry != null && lastEntry.isCompleted()) {
            safepoints.add(lastEntry);
        }
        lastEntry = new SafepointParserOperationLog(timeStamp, sequenceId++, applicationTime);
    }

    void addOperationName(String operationName) {
        if (lastEntry == null) {
            return;
        }
        lastEntry.addOperationName(operationName);
    }

    void addTimeToSafepointAndStoppedTime(BigDecimal timeToSafepoint, BigDecimal stoppedTime) {
        if (lastEntry == null) {
            return;
        }
        lastEntry.addTimeToSafepointAndStoppedTime(timeToSafepoint, stoppedTime);
    }

    void addAllData(BigDecimal timeToSafepoint, BigDecimal stoppedTime, BigDecimal applicationTime, String operationName) {
        if (lastEntry == null) {
            return;
        }
        lastEntry.addAllData(timeToSafepoint, stoppedTime, applicationTime, operationName);
    }
    
    @Override
    public SafepointLog get() {
        if (lastEntry != null && lastEntry.isCompleted()) {
            safepoints.add(lastEntry);
            lastEntry = null;
        }

        return SafepointLog.builder()
                .safepoints(safepoints.stream()
                        .map(this::map)
                        .toList())
                .build();
    }

    private SafepointOperationLog map(SafepointParserOperationLog safepointParserOperationLog) {
        return SafepointOperationLog.builder()
                .applicationTime(safepointParserOperationLog.getApplicationTime())
                .stoppedTime(safepointParserOperationLog.getStoppedTime())
                .ttsTime(safepointParserOperationLog.getTtsTime())
                .sequenceId(safepointParserOperationLog.getSequenceId())
                .timeStamp(safepointParserOperationLog.getTimeStamp())
                .operationName(safepointParserOperationLog.getOperationName())
                .build();
    }
}
