package pl.ks.jvm.logs.parser.custom.safepoint;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import pl.ks.jvm.logs.model.safepoint.SafepointLog;
import pl.ks.jvm.logs.model.safepoint.SafepointOperationLog;

class SafepointParserTest {
    SafepointParser parser = new SafepointParser();

    @Test
    void shouldParseJdk11SingleEntryLog() {
        String log = """
                [0,036s][info][safepoint] Application time: 0,0006619 seconds
                [0,036s][info][safepoint] Entering safepoint region: RevokeBias
                [0,036s][info][safepoint] Leaving safepoint region
                [0,036s][info][safepoint] Total time for which application threads were stopped: 0,0000551 seconds, Stopping threads took: 0,0000146 seconds
                """;

        SafepointLog safepointLog = runParser(log);
        assertEquals(1, safepointLog.getSafepoints().size());

        SafepointOperationLog safepointOperationLog = safepointLog.getSafepoints().get(0);
        assertEquals(new BigDecimal("0.036"), safepointOperationLog.getTimeStamp());
        assertEquals("RevokeBias", safepointOperationLog.getOperationName());
        assertEquals(new BigDecimal("0.0000146"), safepointOperationLog.getTtsTime());
        assertEquals(new BigDecimal("0.0000551"), safepointOperationLog.getStoppedTime());
        assertEquals(new BigDecimal("0.0006619"), safepointOperationLog.getApplicationTime());
    }

    @Test
    void shouldParseJdk11MultiEntryLog() {
        String log = """
                [0,036s][info][safepoint] Application time: 0,0006619 seconds
                [0,036s][info][safepoint] Entering safepoint region: RevokeBias
                [0,036s][info][safepoint] Leaving safepoint region
                [0,036s][info][safepoint] Total time for which application threads were stopped: 0,0000551 seconds, Stopping threads took: 0,0000146 seconds
                [0,051s][info][safepoint] Application time: 0,0147643 seconds
                [0,051s][info][safepoint] Entering safepoint region: Deoptimize
                [0,051s][info][safepoint] Leaving safepoint region
                [0,051s][info][safepoint] Total time for which application threads were stopped: 0,0000611 seconds, Stopping threads took: 0,0000033 seconds                                
                """;

        SafepointLog safepointLog = runParser(log);
        assertEquals(2, safepointLog.getSafepoints().size());
    }

    @Test
    void shouldParseJdk11MultiEntryLogAndSkipUnfinished() {
        String log = """
                [0,036s][info][safepoint] Application time: 0,0006619 seconds
                [0,036s][info][safepoint] Entering safepoint region: RevokeBias
                [0,036s][info][safepoint] Leaving safepoint region
                [0,036s][info][safepoint] Total time for which application threads were stopped: 0,0000551 seconds, Stopping threads took: 0,0000146 seconds
                [0,051s][info][safepoint] Application time: 0,0147643 seconds
                [0,051s][info][safepoint] Entering safepoint region: Deoptimize
                [0,051s][info][safepoint] Leaving safepoint region
                """;

        SafepointLog safepointLog = runParser(log);
        assertEquals(1, safepointLog.getSafepoints().size());
    }

    @Test
    void shouldParseJdk17SingleEntryLog() {
        String log = """
                [0,826s][info][safepoint] Safepoint "G1Concurrent", Time since last: 7880751 ns, Reaching safepoint: 93610 ns, At safepoint: 955684 ns, Total: 1049294 ns
                """;

        SafepointLog safepointLog = runParser(log);
        assertEquals(1, safepointLog.getSafepoints().size());

        SafepointOperationLog safepointOperationLog = safepointLog.getSafepoints().get(0);
        assertEquals(new BigDecimal("0.826"), safepointOperationLog.getTimeStamp());
        assertEquals("G1Concurrent", safepointOperationLog.getOperationName());
        assertEquals(new BigDecimal("0.0000936100"), safepointOperationLog.getTtsTime());
        assertEquals(new BigDecimal("0.0010492940"), safepointOperationLog.getStoppedTime());
        assertEquals(new BigDecimal("0.0078807510"), safepointOperationLog.getApplicationTime());
    }

    @Test
    void shouldParseJdk17MultiEntryLog() {
        String log = """
                [0,826s][info][safepoint] Safepoint "G1Concurrent", Time since last: 7880751 ns, Reaching safepoint: 93610 ns, At safepoint: 955684 ns, Total: 1049294 ns
                [0,827s][info][safepoint] Safepoint "G1Concurrent", Time since last: 1732496 ns, Reaching safepoint: 96121 ns, At safepoint: 57310 ns, Total: 153431 ns
                [1,916s][info][safepoint] Safepoint "Cleanup", Time since last: 1088147317 ns, Reaching safepoint: 150760 ns, At safepoint: 2520 ns, Total: 153280 ns                
                """;

        SafepointLog safepointLog = runParser(log);

        assertEquals(3, safepointLog.getSafepoints().size());
    }

    private SafepointLog runParser(String log) {
        for (String line : log.split("\n")) {
            parser.parseLine(line.trim());
        }
        SafepointLog safepointLog = parser.get();
        return safepointLog;
    }
}
