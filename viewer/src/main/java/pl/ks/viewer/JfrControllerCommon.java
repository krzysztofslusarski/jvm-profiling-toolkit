package pl.ks.viewer;

import pl.ks.jfr.parser.tuning.AdditionalLevel;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

abstract class JfrControllerCommon {
    public static final String ON = "on";

    static JfrViewerFilterAndLevelConfig createConfig(Map<String, String> params) {
        JfrViewerFilterAndLevelConfig.JfrViewerFilterAndLevelConfigBuilder builder = JfrViewerFilterAndLevelConfig.builder();

        boolean consumeCpuOn = ON.equals(params.get("consumeCpuOn"));
        builder.consumeCpuOn(consumeCpuOn);

        boolean reverseOn = ON.equals(params.get("reverseOn"));
        builder.reverseOn(reverseOn);

        boolean threadFilterOn = ON.equals(params.get("threadFilterOn"));
        builder.threadFilterOn(threadFilterOn);
        if (threadFilterOn) {
            builder.threadFilter(params.get("threadFilter"));
        }

        boolean endDurationOn = ON.equals(params.get("endDurationOn"));
        builder.endDurationOn(endDurationOn);
        if (endDurationOn) {
            builder
                    .duration(Long.parseLong(params.get("duration")))
                    .endDate(params.get("endDate"))
                    .endDateDateTimeFormat(params.get("endDateDateTimeFormat"))
                    .localeLanguage(params.get("localeLanguage"));
        }

        boolean warmupCooldownOn = ON.equals(params.get("warmupCooldownOn"));
        builder.warmupCooldownOn(warmupCooldownOn);
        if (warmupCooldownOn) {
            builder
                    .cooldown(Integer.parseInt(params.get("cooldown")))
                    .warmup(Integer.parseInt(params.get("warmup")));
        }

        boolean warmupDurationOn = ON.equals(params.get("warmupDurationOn"));
        builder.warmupDurationOn(warmupDurationOn);
        if (warmupDurationOn) {
            builder
                    .wdDuration(Long.parseLong(params.get("wdDuration")))
                    .wdWarmup(Integer.parseInt(params.get("wdWarmup")));
        }

        boolean stackTraceFilterOn = ON.equals(params.get("stackTraceFilterOn"));
        builder.stackTraceFilterOn(stackTraceFilterOn);
        if (stackTraceFilterOn) {
            builder.stackTraceFilter(params.get("stackTraceFilter"));
        }

        boolean ecidFilterOn = ON.equals(params.get("ecidFilterOn"));
        builder.ecidFilterOn(ecidFilterOn);
        if (ecidFilterOn) {
            builder.ecidFilter(Long.parseLong(params.get("ecidFilter")));
        }

        boolean startEndTimestampOn = ON.equals(params.get("startEndTimestampOn"));
        builder.startEndTimestampOn(startEndTimestampOn);
        if (startEndTimestampOn) {
            builder.startTs(Long.parseLong(params.get("startTs")));
            builder.endTs(Long.parseLong(params.get("endTs")));
        }

        Set<AdditionalLevel> additionalLevels = new HashSet<>();
        if (ON.equals(params.get("extractThreads"))) {
            additionalLevels.add(AdditionalLevel.THREAD);
        }
        if (ON.equals(params.get("extractTs10S"))) {
            additionalLevels.add(AdditionalLevel.TIMESTAMP_10_S);
        }
        if (ON.equals(params.get("extractTs1S"))) {
            additionalLevels.add(AdditionalLevel.TIMESTAMP_1_S);
        }
        if (ON.equals(params.get("extractTs100Ms"))) {
            additionalLevels.add(AdditionalLevel.TIMESTAMP_100_MS);
        }
        if (ON.equals(params.get("extractFilename"))) {
            additionalLevels.add(AdditionalLevel.FILENAME);
        }
        if (ON.equals(params.get("extractEcid"))) {
            additionalLevels.add(AdditionalLevel.ECID);
        }
        if (ON.equals(params.get("extractLineNumbers"))) {
            additionalLevels.add(AdditionalLevel.LINE_NUMBERS);
        }
        builder.additionalLevels(additionalLevels);

        String tableLimit = params.get("tableLimit");
        builder.tableLimit(tableLimit == null ? 0 : Integer.parseInt(tableLimit));

        return builder.build();
    }
}
