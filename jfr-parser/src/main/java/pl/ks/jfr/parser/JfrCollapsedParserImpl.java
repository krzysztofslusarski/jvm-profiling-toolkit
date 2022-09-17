/*
 * Copyright 2022 Krzysztof Slusarski
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package pl.ks.jfr.parser;

import static pl.ks.jfr.parser.JfrParserHelper.fetchFlatStackTrace;
import static pl.ks.jfr.parser.JfrParserHelper.isAsyncAllocNewTLABEvent;
import static pl.ks.jfr.parser.JfrParserHelper.isAsyncAllocOutsideTLABEvent;
import static pl.ks.jfr.parser.JfrParserHelper.isCpuInfoEvent;
import static pl.ks.jfr.parser.JfrParserHelper.isCpuLoadEvent;
import static pl.ks.jfr.parser.JfrParserHelper.isExecutionSampleEvent;
import static pl.ks.jfr.parser.JfrParserHelper.isInitialSystemProperty;
import static pl.ks.jfr.parser.JfrParserHelper.isJvmInfoEvent;
import static pl.ks.jfr.parser.JfrParserHelper.isLockEvent;
import static pl.ks.jfr.parser.JfrParserHelper.isOsInfoEvent;
import static pl.ks.jfr.parser.ParserUtil.getFlightRecording;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.Path;
import java.sql.Date;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;
import lombok.extern.slf4j.Slf4j;
import org.openjdk.jmc.common.item.IAccessorKey;
import org.openjdk.jmc.common.item.IItem;
import org.openjdk.jmc.common.item.IMemberAccessor;
import org.openjdk.jmc.common.unit.IQuantity;
import org.openjdk.jmc.common.unit.ITypedQuantity;
import org.openjdk.jmc.flightrecorder.JfrAttributes;
import org.openjdk.jmc.flightrecorder.internal.EventArray;
import org.openjdk.jmc.flightrecorder.internal.EventArrays;
import org.springframework.util.StopWatch;
import pl.ks.jfr.parser.tuning.AdditionalLevel;
import pl.ks.jfr.parser.tuning.PreStackFilter;

@Slf4j
class JfrCollapsedParserImpl implements JfrCollapsedParser {
    public static final ThreadLocal<SimpleDateFormat> OUTPUT_FORMAT = ThreadLocal.withInitial(() -> new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS", Locale.US));
    public static final ThreadLocal<DecimalFormat> TIME_STAMP_FORMAT = ThreadLocal.withInitial(() -> new DecimalFormat("0000000000000"));
    private static final BigDecimal PERCENT_MULTIPLIER = new BigDecimal(100);

    @Override
    public JfrCollapsedParsedFile parseCollapsed(List<Path> jfrFiles, List<PreStackFilter> filters, Set<AdditionalLevel> additionalLevels) {
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        JfrCollapsedParsedFile jfrCollapsedParsedFile = new JfrCollapsedParsedFile();
        jfrFiles.forEach(path -> parseFile(
                JfrCollapsedParserContext.builder()
                        .preStackFilters(filters)
                        .additionalLevels(additionalLevels)
                        .file(path)
                        .jfrCollapsedParsedFile(jfrCollapsedParsedFile)
                        .build()
        ));
        stopWatch.stop();
        log.info("Parsing took: {}ms", stopWatch.getLastTaskTimeMillis());
        return jfrCollapsedParsedFile;
    }

    @Override
    public StartEndDate calculateDatesWithCoolDownAndWarmUp(Stream<Path> paths, int warmUp, int coolDown) {
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();

        StartEndDateCalculator startEndDateCalculator = new StartEndDateCalculator();

        try {
            for (Path path : paths.toList()) {
                EventArrays flightRecording = getFlightRecording(path);
                for (EventArray eventArray : flightRecording.getArrays()) {
                    if (!isExecutionSampleEvent(eventArray) && !isLockEvent(eventArray) && !isAsyncAllocNewTLABEvent(eventArray) && !isAsyncAllocOutsideTLABEvent(eventArray)) {
                        continue;
                    }
                    IMemberAccessor<IQuantity, IItem> startTimeAccessor = JfrAttributes.START_TIME.getAccessor(eventArray.getType());

                    Arrays.stream(eventArray.getEvents()).parallel()
                            .forEach(event -> {
                                        long startTimestamp = startTimeAccessor.getMember(event).longValue();
                                        Instant eventDate = Instant.ofEpochMilli(startTimestamp / 1000000);
                                        startEndDateCalculator.newDate(eventDate);
                                    }
                            );
                }
            }

            stopWatch.stop();
            log.info("Calculating dates took: {}ms", stopWatch.getLastTaskTimeMillis());
            return StartEndDate.builder()
                    .startDate(startEndDateCalculator.getStartDate().plus(warmUp, ChronoUnit.SECONDS))
                    .endDate(startEndDateCalculator.getEndDate().minus(coolDown, ChronoUnit.SECONDS))
                    .build();
        } catch (Exception e) {
            log.error("Fatal error", e);
            throw new RuntimeException(e);
        }
    }


    private static void parseFile(JfrCollapsedParserContext context) {
        log.info("Input file: " + context.getFile().getFileName());
        log.info("Converting JFR to collapsed stack ...");

        JfrCollapsedParsedFile jfrCollapsedParsedFile = context.getJfrCollapsedParsedFile();

        try {
            EventArrays flightRecording = getFlightRecording(context.getFile());

            for (EventArray eventArray : flightRecording.getArrays()) {
                if (isExecutionSampleEvent(eventArray)) {
                    processWallEvent(context, eventArray);
                } else if (isLockEvent(eventArray)) {
                    processLockEvent(context, eventArray);
                } else if (isAsyncAllocNewTLABEvent(eventArray)) {
                    processAllocEvent(context, eventArray, false);
                } else if (isAsyncAllocOutsideTLABEvent(eventArray)) {
                    processAllocEvent(context, eventArray, true);
                } else if (isCpuLoadEvent(eventArray)) {
                    processCpuEvent(context, eventArray);
                } else if (isOsInfoEvent(eventArray)) {
                    processEventValueToMap(jfrCollapsedParsedFile.getOsInfo(), eventArray);
                } else if (isCpuInfoEvent(eventArray)) {
                    processEventValueToMap(jfrCollapsedParsedFile.getCpuInfo(), eventArray);
                } else if (isJvmInfoEvent(eventArray)) {
                    processEventValueToMap(jfrCollapsedParsedFile.getJvmInfo(), eventArray);
                } else if (isInitialSystemProperty(eventArray)) {
                    processKVEventValueToMap(jfrCollapsedParsedFile.getInitialSystemProperties(), eventArray);
                }
            }
        } catch (Exception e) {
            log.error("Fatal error", e);
            throw new RuntimeException(e);
        }
    }

    private static void processKVEventValueToMap(Map<String, String> map, EventArray eventArray) {
        IMemberAccessor<String, IItem> keyAccessor = JfrParserHelper.findKeyAccessor(eventArray);
        IMemberAccessor<String, IItem> valueAccessor = JfrParserHelper.findValueAccessor(eventArray);

        Arrays.stream(eventArray.getEvents()).forEach(event -> {
            map.put(keyAccessor.getMember(event), valueAccessor.getMember(event));
        });
    }

    private static void processEventValueToMap(Map<String, String> map, EventArray eventArray) {
        Arrays.stream(eventArray.getEvents()).forEach(event -> {
            for (IAccessorKey<?> accessor : eventArray.getType().getAccessorKeys().keySet()) {
                String key = accessor.getIdentifier();
                IMemberAccessor<?, IItem> valueAccessor = eventArray.getType().getAccessor(accessor);
                Object objValue = valueAccessor.getMember(event);
                if (objValue instanceof String) {
                    map.put(key, objValue.toString());
                } else if (objValue instanceof IQuantity) {
                    IQuantity iQuantity = (IQuantity) objValue;
                    if ("timestamp".equals(iQuantity.getType().getIdentifier())) {
                        long startTimestamp = iQuantity.longValue();
                        Instant eventDate = iQuantity.getUnit().getIdentifier().equals("epochms") ?
                                Instant.ofEpochMilli(startTimestamp) :
                                Instant.ofEpochMilli(startTimestamp / 1000000);
                        map.put(key, OUTPUT_FORMAT.get().format(Date.from(eventDate)));
                    } else {
                        map.put(key, objValue.toString().replace("[]", ""));
                    }
                }
            }
        });
    }

    private static void processCpuEvent(JfrCollapsedParserContext context, EventArray eventArray) {
        JfrCollapsedParsedFile jfrCollapsedParsedFile = context.getJfrCollapsedParsedFile();
        List<PreStackFilter> preStackFilters = context.getPreStackFilters();

        JfrAccessors accessors = JfrAccessors.builder()
                .startTimeAccessor(JfrAttributes.START_TIME.getAccessor(eventArray.getType()))
                .jvmSystemAccessor(JfrParserHelper.findCpuJvmSystemAccessor(eventArray))
                .jvmUserAccessor(JfrParserHelper.findCpuJvmUserAccessor(eventArray))
                .machineTotalAccessor(JfrParserHelper.findMachineTotalAccessor(eventArray))
                .build();
        Arrays.stream(eventArray.getEvents()).parallel().forEach(event -> {
            if (shouldSkipByFilter(preStackFilters, accessors, event)) {
                return;
            }

            ITypedQuantity jvmUser = accessors.getJvmUserAccessor().getMember(event);
            ITypedQuantity jvmSystem = accessors.getJvmSystemAccessor().getMember(event);
            ITypedQuantity machineTotal = accessors.getMachineTotalAccessor().getMember(event);

            int scaledJvmUser = BigDecimal.valueOf(jvmUser.doubleValue()).multiply(PERCENT_MULTIPLIER).setScale(0, RoundingMode.HALF_EVEN).intValue();
            int scaledJvmSystem = BigDecimal.valueOf(jvmSystem.doubleValue()).multiply(PERCENT_MULTIPLIER).setScale(0, RoundingMode.HALF_EVEN).intValue();
            int scaledJvmTotal = scaledJvmSystem + scaledJvmUser;
            int scaledMachineTotal = BigDecimal.valueOf(machineTotal.doubleValue()).multiply(PERCENT_MULTIPLIER).setScale(0, RoundingMode.HALF_EVEN).intValue();

            jfrCollapsedParsedFile.getCpuLoadJvmUser().addSingleStack(createCpuLoadStack(context, "JVM user", scaledJvmUser));
            jfrCollapsedParsedFile.getCpuLoadJvmSystem().addSingleStack(createCpuLoadStack(context, "JVM system", scaledJvmSystem));
            jfrCollapsedParsedFile.getCpuLoadJvmTotal().addSingleStack(createCpuLoadStack(context, "JVM total", scaledJvmTotal));
            jfrCollapsedParsedFile.getCpuLoadMachineTotal().addSingleStack(createCpuLoadStack(context, "Machine total", scaledMachineTotal));
            jfrCollapsedParsedFile.getCpuLoadMachineTotalMinusJvmTotal().addSingleStack(createCpuLoadStack(context, "Machine total - JVM total", scaledMachineTotal - scaledJvmTotal));
        });
    }

    private static String createCpuLoadStack(JfrCollapsedParserContext context, String prefix, int counter) {
        if (counter < 0) {
            counter = 0;
        }
        StringBuilder builder = new StringBuilder(prefix);
        if (context.isIncludeFileName()) {
            String filename = context.getFile().getFileName().toString();
            builder.append(";").append(filename).append("_[i]");
        }

        for (int i = 0; i <= counter; i++) {
            builder.append(";").append(i).append("%");
            if (i <= 25) {
                builder.append("_[i]");
            } else if (i <= 50) {
                builder.append("_[j]");
            } else if (i <= 75) {
                builder.append("_[k]");
            }
        }

        return builder.toString();
    }


    private static void processAllocEvent(JfrCollapsedParserContext context, EventArray eventArray, boolean outsideTlab) {
        JfrCollapsedParsedFile jfrCollapsedParsedFile = context.getJfrCollapsedParsedFile();
        List<PreStackFilter> preStackFilters = context.getPreStackFilters();

        JfrAccessors accessors = JfrAccessors.builder()
                .stackTraceAccessor(JfrAttributes.EVENT_STACKTRACE.getAccessor(eventArray.getType()))
                .threadAccessor(JfrAttributes.EVENT_THREAD.getAccessor(eventArray.getType()))
                .startTimeAccessor(JfrAttributes.START_TIME.getAccessor(eventArray.getType()))
                .allocationSizeAccessor(JfrParserHelper.findAllocSizeAccessor(eventArray))
                .objectClassAccessor(JfrParserHelper.findObjectClassAccessor(eventArray))
                .ecidAccessor(JfrParserHelper.findEcidAccessor(eventArray))
                .build();

        Arrays.stream(eventArray.getEvents()).parallel().forEach(event -> {
            if (shouldSkipByFilter(preStackFilters, accessors, event)) {
                return;
            }

            String objectClass = accessors.getObjectClassAccessor().getMember(event).getFullName();
            String stacktrace = fetchFlatStackTrace(event, accessors, context) + ";" + objectClass + (outsideTlab ? "_[i]" : "_[k]");
            long size = accessors.getAllocationSizeAccessor().getMember(event).longValue();
            jfrCollapsedParsedFile.getAllocCount().addSingleStack(stacktrace);
            jfrCollapsedParsedFile.getAllocSize().add(stacktrace, size);
        });
    }

    private static void processLockEvent(JfrCollapsedParserContext context, EventArray eventArray) {
        JfrCollapsedParsedFile jfrCollapsedParsedFile = context.getJfrCollapsedParsedFile();
        List<PreStackFilter> preStackFilters = context.getPreStackFilters();

        JfrAccessors accessors = JfrAccessors.builder()
                .stackTraceAccessor(JfrAttributes.EVENT_STACKTRACE.getAccessor(eventArray.getType()))
                .threadAccessor(JfrAttributes.EVENT_THREAD.getAccessor(eventArray.getType()))
                .startTimeAccessor(JfrAttributes.START_TIME.getAccessor(eventArray.getType()))
                .monitorClassAccessor(JfrParserHelper.findMonitorClassAccessor(eventArray))
                .ecidAccessor(JfrParserHelper.findEcidAccessor(eventArray))
                .build();

        Arrays.stream(eventArray.getEvents()).parallel().forEach(event -> {
            if (shouldSkipByFilter(preStackFilters, accessors, event)) {
                return;
            }

            String monitorClass = accessors.getMonitorClassAccessor().getMember(event).getFullName();
            String stacktrace = fetchFlatStackTrace(event, accessors, context) + ";" + monitorClass + "_[i]";
            jfrCollapsedParsedFile.getLock().addSingleStack(stacktrace);
        });
    }

    private static void processWallEvent(JfrCollapsedParserContext context, EventArray eventArray) {
        JfrCollapsedParsedFile jfrCollapsedParsedFile = context.getJfrCollapsedParsedFile();
        List<PreStackFilter> preStackFilters = context.getPreStackFilters();

        JfrAccessors accessors = JfrAccessors.builder()
                .stackTraceAccessor(JfrAttributes.EVENT_STACKTRACE.getAccessor(eventArray.getType()))
                .threadAccessor(JfrAttributes.EVENT_THREAD.getAccessor(eventArray.getType()))
                .startTimeAccessor(JfrAttributes.START_TIME.getAccessor(eventArray.getType()))
                .stateAccessor(JfrParserHelper.findStateAccessor(eventArray))
                .ecidAccessor(JfrParserHelper.findEcidAccessor(eventArray))
                .build();

        Arrays.stream(eventArray.getEvents()).parallel().forEach(event -> {
            if (shouldSkipByFilter(preStackFilters, accessors, event)) {
                return;
            }

            boolean consumesCpu = accessors.getStateAccessor() != null &&
                    JfrParserHelper.isConsumingCpu(accessors.getStateAccessor().getMember(event));

            if (accessors.getEcidAccessor() != null) {
                long ecid = accessors.getEcidAccessor().getMember(event).longValue();
                if (ecid != 0) {
                    long startTimestamp = accessors.getStartTimeAccessor().getMember(event).longValue();
                    Instant eventDate = Instant.ofEpochMilli(startTimestamp / 1000000);
                    jfrCollapsedParsedFile.getEcidInfo().computeIfAbsent(ecid, JfrEcidInfo::new).newExecutionSample(eventDate, consumesCpu);
                }
            }

            String stacktrace = fetchFlatStackTrace(event, accessors, context);
            jfrCollapsedParsedFile.getWall().addSingleStack(stacktrace);
            if (consumesCpu) {
                jfrCollapsedParsedFile.getCpu().addSingleStack(stacktrace);
            }
        });
    }

    private static boolean shouldSkipByFilter(List<PreStackFilter> preStackFilters, JfrAccessors accessors, IItem event) {
        for (PreStackFilter preStackFilter : preStackFilters) {
            if (!preStackFilter.shouldInclude(accessors, event)) {
                return true;
            }
        }
        return false;
    }
}
