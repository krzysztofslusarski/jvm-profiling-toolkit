package pl.ks.jfr.parser;

import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import pl.ks.jfr.parser.tuning.AdditionalLevel;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import static pl.ks.jfr.parser.tuning.AdditionalLevel.ECID;
import static pl.ks.jfr.parser.tuning.AdditionalLevel.FILENAME;
import static pl.ks.jfr.parser.tuning.AdditionalLevel.LINE_NUMBERS;
import static pl.ks.jfr.parser.tuning.AdditionalLevel.THREAD;
import static pl.ks.jfr.parser.tuning.AdditionalLevel.TIMESTAMP_100_MS;
import static pl.ks.jfr.parser.tuning.AdditionalLevel.TIMESTAMP_10_S;
import static pl.ks.jfr.parser.tuning.AdditionalLevel.TIMESTAMP_1_S;

public interface JfrParsedCommonStackTraceEvent extends JfrParsedEventWithTime {
    ThreadLocal<SimpleDateFormat> OUTPUT_FORMAT = ThreadLocal.withInitial(() -> new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS", Locale.US));
    ThreadLocal<DateTimeFormatter> OUTPUT_FORMAT_DTF = ThreadLocal.withInitial(() -> DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS", Locale.US));
    ThreadLocal<DecimalFormat> TIME_STAMP_FORMAT = ThreadLocal.withInitial(() -> new DecimalFormat("0000000000000"));

    String[] FRAME_SUFFIX = {"_[0]", "_[j]", "_[i]", "_[k]", "_[1]"};
    ZoneId SYSTEM_DEFAULT_ZONE_ID = ZoneId.systemDefault();
    ZoneId UTC_ZONE_ID = ZoneId.of("UTC");

    String[] getStackTrace();

    int[] getLineNumbers();

    long getCorrelationId();

    String getThreadName();

    String getFilename();

    Instant getEventTime();

    default boolean stackTraceContains(String part) {
        for (String frame : getStackTrace()) {
            if (frame.contains(part)) {
                return true;
            }
        }
        return false;
    }

    default boolean stackTraceNotContains(String part) {
        return !stackTraceContains(part);
    }

    default void addCommonStackTraceElements(List<String[]> fullStackTrace, Set<AdditionalLevel> additionalLevels) {
        if (additionalLevels.contains(ECID)) {
            fullStackTrace.add(new String[]{String.valueOf(getCorrelationId())});
        }
        if (additionalLevels.contains(TIMESTAMP_100_MS)) {
            long time = getEventTime().toEpochMilli() / 100;
            Date date = new Date(time * 100);
            ZonedDateTime utc = OffsetDateTime.from(date.toInstant().atZone(SYSTEM_DEFAULT_ZONE_ID)).atZoneSameInstant(UTC_ZONE_ID);
            fullStackTrace.add(new String[]{
                    TIME_STAMP_FORMAT.get().format(time) + "_" + OUTPUT_FORMAT_DTF.get().format(utc) + " UTC_[k]"
            });
            fullStackTrace.add(new String[]{
                    TIME_STAMP_FORMAT.get().format(time) + "_" + OUTPUT_FORMAT.get().format(date) + " " + SYSTEM_DEFAULT_ZONE_ID +  "_[k]"
            });
        }
        if (additionalLevels.contains(TIMESTAMP_1_S)) {
            long time = getEventTime().toEpochMilli() / 1000;
            Date date = new Date(time * 1000);
            ZonedDateTime utc = OffsetDateTime.from(date.toInstant().atZone(SYSTEM_DEFAULT_ZONE_ID)).atZoneSameInstant(UTC_ZONE_ID);
            fullStackTrace.add(new String[]{
                    TIME_STAMP_FORMAT.get().format(time) + "_" + OUTPUT_FORMAT_DTF.get().format(utc) + " UTC_[k]"
            });
            fullStackTrace.add(new String[]{
                    TIME_STAMP_FORMAT.get().format(time) + "_" + OUTPUT_FORMAT.get().format(date) + " " + SYSTEM_DEFAULT_ZONE_ID +  "_[k]"
            });
        }
        if (additionalLevels.contains(TIMESTAMP_10_S)) {
            long time = getEventTime().toEpochMilli() / 10000;
            Date date = new Date(time * 10000);
            ZonedDateTime utc = OffsetDateTime.from(date.toInstant().atZone(SYSTEM_DEFAULT_ZONE_ID)).atZoneSameInstant(UTC_ZONE_ID);
            fullStackTrace.add(new String[]{
                    TIME_STAMP_FORMAT.get().format(time) + "_" + OUTPUT_FORMAT_DTF.get().format(utc) + " UTC_[k]"
            });
            fullStackTrace.add(new String[]{
                    TIME_STAMP_FORMAT.get().format(time) + "_" + OUTPUT_FORMAT.get().format(date) + " " + SYSTEM_DEFAULT_ZONE_ID +  "_[k]"
            });
        }
        if (additionalLevels.contains(FILENAME)) {
            fullStackTrace.add(new String[]{getFilename() + "_[i]"});
        }
        if (additionalLevels.contains(THREAD)) {
            fullStackTrace.add(new String[]{getThreadName()});
        }
        if (additionalLevels.contains(LINE_NUMBERS)) {
            String[] withLines = new String[getStackTrace().length];
            for (int i = 0; i < withLines.length; i++) {
                String frame = getStackTrace()[i];
                int lineNumber = getLineNumbers()[i];
                if (lineNumber < 0) {
                    withLines[i] = frame;
                } else {
                    withLines[i] = frame + ":" + lineNumber;
                    for (String frameSuffix : FRAME_SUFFIX) {
                        if (frame.endsWith(frameSuffix)) {
                            withLines[i] = frame.substring(0, frame.length() - frameSuffix.length()) + ":" + lineNumber + frameSuffix;
                            break;
                        }
                    }
                }
            }
            fullStackTrace.add(withLines);
        } else {
            fullStackTrace.add(getStackTrace());
        }
    }
}
