package pl.ks.viewer;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import pl.ks.jfr.parser.tuning.AdditionalLevel;

abstract class JfrControllerCommon {
    static JfrViewerFilterAndLevelConfig createConfig(Map<String, String> params) {
        boolean threadFilterOn = JfrViewerController.ON.equals(params.get("threadFilterOn"));
        JfrViewerFilterAndLevelConfig.JfrViewerFilterAndLevelConfigBuilder builder = JfrViewerFilterAndLevelConfig.builder();
        builder.threadFilterOn(threadFilterOn);
        if (threadFilterOn) {
            builder.threadFilter(params.get("threadFilter"));
        }

        boolean endDurationOn = JfrViewerController.ON.equals(params.get("endDurationOn"));
        builder.endDurationOn(endDurationOn);
        if (endDurationOn) {
            builder
                    .duration(Long.parseLong(params.get("duration")))
                    .endDate(params.get("endDate"))
                    .endDateDateTimeFormat(params.get("endDateDateTimeFormat"));
        }

        boolean warmupCooldownOn = JfrViewerController.ON.equals(params.get("warmupCooldownOn"));
        builder.warmupCooldownOn(warmupCooldownOn);
        if (warmupCooldownOn) {
            builder
                    .cooldown(Integer.parseInt(params.get("cooldown")))
                    .warmup(Integer.parseInt(params.get("warmup")));
        }

        boolean warmupDurationOn = JfrViewerController.ON.equals(params.get("warmupDurationOn"));
        builder.warmupDurationOn(warmupDurationOn);
        if (warmupDurationOn) {
            builder
                    .wdDuration(Long.parseLong(params.get("wdDuration")))
                    .wdWarmup(Integer.parseInt(params.get("wdWarmup")));
        }

        boolean ecidFilterOn = JfrViewerController.ON.equals(params.get("ecidFilterOn"));
        builder.ecidFilterOn(ecidFilterOn);
        if (ecidFilterOn) {
            builder.ecidFilter(Long.parseLong(params.get("ecidFilter")));
        }

        boolean startEndTimestampOn = JfrViewerController.ON.equals(params.get("startEndTimestampOn"));
        builder.startEndTimestampOn(startEndTimestampOn);
        if (startEndTimestampOn) {
            builder.startTs(Long.parseLong(params.get("startTs")));
            builder.endTs(Long.parseLong(params.get("endTs")));
        }

        Set<AdditionalLevel> additionalLevels = new HashSet<>();
        if (JfrViewerController.ON.equals(params.get("extractThreads"))) {
            additionalLevels.add(AdditionalLevel.THREAD);
        }
        if (JfrViewerController.ON.equals(params.get("extractTs10S"))) {
            additionalLevels.add(AdditionalLevel.TIMESTAMP_10_S);
        }
        if (JfrViewerController.ON.equals(params.get("extractTs1S"))) {
            additionalLevels.add(AdditionalLevel.TIMESTAMP_1_S);
        }
        if (JfrViewerController.ON.equals(params.get("extractTs100Ms"))) {
            additionalLevels.add(AdditionalLevel.TIMESTAMP_100_MS);
        }
        if (JfrViewerController.ON.equals(params.get("extractFilename"))) {
            additionalLevels.add(AdditionalLevel.FILENAME);
        }
        if (JfrViewerController.ON.equals(params.get("extractEcid"))) {
            additionalLevels.add(AdditionalLevel.ECID);
        }
        builder.additionalLevels(additionalLevels);

        return builder.build();
    }
}
