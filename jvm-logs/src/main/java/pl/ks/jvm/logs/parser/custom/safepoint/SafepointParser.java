package pl.ks.jvm.logs.parser.custom.safepoint;

import pl.ks.jvm.logs.model.safepoint.SafepointLog;
import pl.ks.jvm.logs.parser.JvmLogEntryProvider;
import pl.ks.jvm.logs.parser.LineByLineJvmLogParser;
import pl.ks.jvm.logs.parser.ParserUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;

class SafepointParser implements JvmLogEntryProvider<SafepointLog>, LineByLineJvmLogParser {
    private static final BigDecimal NS_TO_MS_DIVISIOR = new BigDecimal(1_000_000);
    private static final BigDecimal TO_MS_MULTIPLIER = new BigDecimal(1000);
    private static final int SCALE = 10;

    private boolean waitForNext = true;
    private final SafepointParserLog log = new SafepointParserLog();

    @Override
    public void parseLine(String line) {
        if (line.contains("Application time")) {
            log.newSafepoint(ParserUtils.getTimeStamp(line), parseApplicationTime(line));
            waitForNext = false;
        } else if (!waitForNext && line.contains("Entering safepoint region")) {
            parseAndAddOperationName(line);
        } else if (!waitForNext && line.contains("Total time for which application threads were stopped")) {
            parseAndAddTtsAndStoppedTime(line);
            waitForNext = true;
        } else if (line.contains("Reaching safepoint")) {
            log.newSafepoint(ParserUtils.getTimeStamp(line));
            parseAndAddJava13OneLine(line);
            waitForNext = false;
        }
    }

    private void parseAndAddJava13OneLine(String line) {
        String name = line
                .replaceFirst(".* Safepoint \"", "")
                .replaceFirst("\", Time since.*", "")
                .trim();
        String tts = line
                .replaceFirst(".* Reaching safepoint: ", "")
                .replaceFirst(" ns, At safepoint.*", "")
                .trim();
        String stopped = line
                .replaceFirst(".*Total: ", "")
                .replaceFirst(" ns", "")
                .trim();
        String appTime = line
                .replaceFirst(".*Time since last: ", "")
                .replaceFirst(" ns, Reaching safepoint.*", "")
                .trim();
        log.addAllData(
                new BigDecimal(tts)
                        .divide(NS_TO_MS_DIVISIOR, SCALE, RoundingMode.HALF_EVEN)
                        .divide(TO_MS_MULTIPLIER, SCALE, RoundingMode.HALF_EVEN),
                new BigDecimal(stopped)
                        .divide(NS_TO_MS_DIVISIOR, SCALE, RoundingMode.HALF_EVEN)
                        .divide(TO_MS_MULTIPLIER, SCALE, RoundingMode.HALF_EVEN),
                new BigDecimal(appTime)
                        .divide(NS_TO_MS_DIVISIOR, SCALE, RoundingMode.HALF_EVEN)
                        .divide(TO_MS_MULTIPLIER, SCALE, RoundingMode.HALF_EVEN),
                name
        );
    }

    private void parseAndAddTtsAndStoppedTime(String line) {
        String tts = line
                .replaceFirst(".*Stopping threads took: ", "")
                .replace(" seconds", "")
                .replace(',', '.')
                .trim();
        String stopped = line
                .replaceFirst(".*Total time for which application threads were stopped: ", "")
                .replaceFirst(" seconds, Stopping threads.*", "")
                .replace(',', '.')
                .trim();

        log.addTimeToSafepointAndStoppedTime(new BigDecimal(tts), new BigDecimal(stopped));
    }

    private void parseAndAddOperationName(String line) {
        String name = line
                .replaceFirst(".*Entering safepoint region: ", "")
                .trim();
        log.addOperationName(name);
    }

    private BigDecimal parseApplicationTime(String line) {
        String appTime = line
                .replaceFirst(".*Application time: ", "")
                .replace(" seconds", "")
                .replace(',', '.')
                .trim();
        return new BigDecimal(appTime);
    }

    @Override
    public SafepointLog get() {
        return log.get();
    }
}
