/*
 * Copyright 2020 Krzysztof Slusarski
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

import lombok.extern.slf4j.Slf4j;
import org.openjdk.jmc.common.IMCStackTrace;
import org.openjdk.jmc.common.IMCThread;
import org.openjdk.jmc.common.IMCType;
import org.openjdk.jmc.common.item.IAccessorKey;
import org.openjdk.jmc.common.item.IItem;
import org.openjdk.jmc.common.item.IMemberAccessor;
import org.openjdk.jmc.common.unit.IQuantity;
import org.openjdk.jmc.common.unit.ITypedQuantity;
import org.openjdk.jmc.flightrecorder.CouldNotLoadRecordingException;
import org.openjdk.jmc.flightrecorder.JfrAttributes;
import org.openjdk.jmc.flightrecorder.internal.EventArray;
import org.openjdk.jmc.flightrecorder.internal.EventArrays;
import org.openjdk.jmc.flightrecorder.internal.FlightRecordingLoader;
import org.springframework.util.StopWatch;
import pl.ks.jfr.parser.filter.PreStackFilter;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Date;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.GZIPInputStream;

import static pl.ks.jfr.parser.JfrParserHelper.fetchFlatStackTrace;
import static pl.ks.jfr.parser.JfrParserHelper.isAsyncAllocNewTLABEvent;
import static pl.ks.jfr.parser.JfrParserHelper.isAsyncAllocOutsideTLABEvent;
import static pl.ks.jfr.parser.JfrParserHelper.isAsyncWallEvent;
import static pl.ks.jfr.parser.JfrParserHelper.isCpuInfoEvent;
import static pl.ks.jfr.parser.JfrParserHelper.isCpuLoadEvent;
import static pl.ks.jfr.parser.JfrParserHelper.isInitialSystemProperty;
import static pl.ks.jfr.parser.JfrParserHelper.isJvmInfoEvent;
import static pl.ks.jfr.parser.JfrParserHelper.isLockEvent;
import static pl.ks.jfr.parser.JfrParserHelper.isOsInfoEvent;

@Slf4j
class JfrParserImpl implements JfrParser {
    private static final SimpleDateFormat OUTPUT_FORMAT = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS", Locale.US);
    private static final BigDecimal PERCENT_MULTIPLIER = new BigDecimal(100);

    @Override
    public JfrParsedFile parse(List<Path> jfrFiles, List<PreStackFilter> filters) {
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        JfrParsedFile jfrParsedFile = new JfrParsedFile();
        jfrFiles.forEach(path -> parseFile(jfrParsedFile, path, filters));
        stopWatch.stop();
        log.info("Parsing took: {}ms", stopWatch.getLastTaskTimeMillis());
        return jfrParsedFile;
    }

    @Override
    public StartEndDate calculateDatesWithCoolDownAndWarmUp(Stream<Path> paths, int warmUp, int coolDown) {
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();

        StartEndDateCalculator startEndDateCalculator = new StartEndDateCalculator();

        try {
            for (Path path : paths.collect(Collectors.toList())) {
                EventArrays flightRecording = getFlightRecording(path);
                for (EventArray eventArray : flightRecording.getArrays()) {
                    if (!isAsyncWallEvent(eventArray) && !isLockEvent(eventArray) && !isAsyncAllocNewTLABEvent(eventArray) && !isAsyncAllocOutsideTLABEvent(eventArray)) {
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


    private static void parseFile(JfrParsedFile jfrParsedFile, Path file, List<PreStackFilter> preStackFilters) {
        log.info("Input file: " + file.getFileName());
        log.info("Converting JFR to collapsed stack ...");

        try {
            EventArrays flightRecording = getFlightRecording(file);

            for (EventArray eventArray : flightRecording.getArrays()) {
                if (isAsyncWallEvent(eventArray)) {
                    processWallEvent(jfrParsedFile, preStackFilters, eventArray);
                } else if (isLockEvent(eventArray)) {
                    processLockEvent(jfrParsedFile, preStackFilters, eventArray);
                } else if (isAsyncAllocNewTLABEvent(eventArray)) {
                    processAllocEvent(jfrParsedFile, preStackFilters, eventArray, false);
                } else if (isAsyncAllocOutsideTLABEvent(eventArray)) {
                    processAllocEvent(jfrParsedFile, preStackFilters, eventArray, true);
                } else if (isCpuLoadEvent(eventArray)) {
                    processCpuEvent(jfrParsedFile, preStackFilters, eventArray);
                } else if (isOsInfoEvent(eventArray)) {
                    processEventValueToMap(jfrParsedFile.getOsInfo(), eventArray);
                } else if (isCpuInfoEvent(eventArray)) {
                    processEventValueToMap(jfrParsedFile.getCpuInfo(), eventArray);
                } else if (isJvmInfoEvent(eventArray)) {
                    processEventValueToMap(jfrParsedFile.getJvmInfo(), eventArray);
                } else if (isInitialSystemProperty(eventArray)) {
                    processKVEventValueToMap(jfrParsedFile.getInitialSystemProperties(), eventArray);
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
                        map.put(key, OUTPUT_FORMAT.format(Date.from(eventDate)));
                    } else {
                        map.put(key, objValue.toString().replace("[]", ""));
                    }
                }
            }
        });
    }

    private static void processCpuEvent(JfrParsedFile jfrParsedFile, List<PreStackFilter> preStackFilters, EventArray eventArray) {
        IMemberAccessor<IQuantity, IItem> startTimeAccessor = JfrAttributes.START_TIME.getAccessor(eventArray.getType());
        IMemberAccessor<ITypedQuantity, IItem> jvmUserAccessor = JfrParserHelper.findCpuJvmUserAccessor(eventArray);
        IMemberAccessor<ITypedQuantity, IItem> jvmSystemAccessor = JfrParserHelper.findCpuJvmSystemAccessor(eventArray);
        IMemberAccessor<ITypedQuantity, IItem> machineTotalAccessor = JfrParserHelper.findMachineTotalAccessor(eventArray);
        Arrays.stream(eventArray.getEvents()).parallel().forEach(event -> {
            if (shouldSkipByFilter(preStackFilters, startTimeAccessor, null, event)) {
                return;
            }

            ITypedQuantity jvmUser = jvmUserAccessor.getMember(event);
            ITypedQuantity jvmSystem = jvmSystemAccessor.getMember(event);
            ITypedQuantity machineTotal = machineTotalAccessor.getMember(event);

            int scaledJvmUser = BigDecimal.valueOf(jvmUser.doubleValue()).multiply(PERCENT_MULTIPLIER).setScale(0, RoundingMode.HALF_EVEN).intValue();
            int scaledJvmSystem = BigDecimal.valueOf(jvmSystem.doubleValue()).multiply(PERCENT_MULTIPLIER).setScale(0, RoundingMode.HALF_EVEN).intValue();
            int scaledJvmTotal = scaledJvmSystem + scaledJvmUser;
            int scaledMachineTotal = BigDecimal.valueOf(machineTotal.doubleValue()).multiply(PERCENT_MULTIPLIER).setScale(0, RoundingMode.HALF_EVEN).intValue();

            jfrParsedFile.getCpuLoad().addSingleStack(createCpuLoadStack("JVM User", scaledJvmUser));
            jfrParsedFile.getCpuLoad().addSingleStack(createCpuLoadStack("JVM System", scaledJvmSystem));
            jfrParsedFile.getCpuLoad().addSingleStack(createCpuLoadStack("JVM Total", scaledJvmTotal));
            jfrParsedFile.getCpuLoad().addSingleStack(createCpuLoadStack("Machine Total", scaledMachineTotal));
        });
    }

    private static String createCpuLoadStack(String prefix, int counter) {
        StringBuilder builder = new StringBuilder(prefix);

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


    private static void processAllocEvent(JfrParsedFile jfrParsedFile, List<PreStackFilter> preStackFilters, EventArray eventArray, boolean outsideTlab) {
        IMemberAccessor<IQuantity, IItem> startTimeAccessor = JfrAttributes.START_TIME.getAccessor(eventArray.getType());
        IMemberAccessor<IMCStackTrace, IItem> stackTraceAccessor = JfrAttributes.EVENT_STACKTRACE.getAccessor(eventArray.getType());
        IMemberAccessor<IMCThread, IItem> threadAccessor = JfrAttributes.EVENT_THREAD.getAccessor(eventArray.getType());
        IMemberAccessor<IQuantity, IItem> allocationSizeAccessor = JfrParserHelper.findAllocSizeAccessor(eventArray);
        IMemberAccessor<IMCType, IItem> objectClassAccessor = JfrParserHelper.findObjectClassAccessor(eventArray);

        Arrays.stream(eventArray.getEvents()).parallel().forEach(event -> {
            if (shouldSkipByFilter(preStackFilters, startTimeAccessor, threadAccessor, event)) {
                return;
            }

            String objectClass = objectClassAccessor.getMember(event).getFullName();
            String stacktrace = fetchFlatStackTrace(event, stackTraceAccessor, threadAccessor) + ";" + objectClass + (outsideTlab ? "_[i]" : "_[k]");
            long size = allocationSizeAccessor.getMember(event).longValue();
            jfrParsedFile.getAllocCount().addSingleStack(stacktrace);
            jfrParsedFile.getAllocSize().add(stacktrace, size);
        });
    }

    private static void processLockEvent(JfrParsedFile jfrParsedFile, List<PreStackFilter> preStackFilters, EventArray eventArray) {
        IMemberAccessor<IQuantity, IItem> startTimeAccessor = JfrAttributes.START_TIME.getAccessor(eventArray.getType());
        IMemberAccessor<IMCStackTrace, IItem> stackTraceAccessor = JfrAttributes.EVENT_STACKTRACE.getAccessor(eventArray.getType());
        IMemberAccessor<IMCThread, IItem> threadAccessor = JfrAttributes.EVENT_THREAD.getAccessor(eventArray.getType());
        IMemberAccessor<IMCType, IItem> monitorClassAccessor = JfrParserHelper.findMonitorClassAccessor(eventArray);

        Arrays.stream(eventArray.getEvents()).parallel().forEach(event -> {
            if (shouldSkipByFilter(preStackFilters, startTimeAccessor, threadAccessor, event)) {
                return;
            }

            String monitorClass = monitorClassAccessor.getMember(event).getFullName();
            String stacktrace = fetchFlatStackTrace(event, stackTraceAccessor, threadAccessor) + ";" + monitorClass + "_[i]";
            jfrParsedFile.getLock().addSingleStack(stacktrace);
        });
    }

    private static void processWallEvent(JfrParsedFile jfrParsedFile, List<PreStackFilter> preStackFilters, EventArray eventArray) {
        IMemberAccessor<IQuantity, IItem> startTimeAccessor = JfrAttributes.START_TIME.getAccessor(eventArray.getType());
        IMemberAccessor<IMCStackTrace, IItem> stackTraceAccessor = JfrAttributes.EVENT_STACKTRACE.getAccessor(eventArray.getType());
        IMemberAccessor<IMCThread, IItem> threadAccessor = JfrAttributes.EVENT_THREAD.getAccessor(eventArray.getType());
        IMemberAccessor<String, IItem> stateAccessor = JfrParserHelper.findStateAccessor(eventArray);

        Arrays.stream(eventArray.getEvents()).parallel().forEach(event -> {
            if (shouldSkipByFilter(preStackFilters, startTimeAccessor, threadAccessor, event)) {
                return;
            }

            String stacktrace = fetchFlatStackTrace(event, stackTraceAccessor, threadAccessor);
            jfrParsedFile.getWall().addSingleStack(stacktrace);
            if (stateAccessor != null && JfrParserHelper.isConsumingCpu(stateAccessor.getMember(event))) {
                jfrParsedFile.getCpu().addSingleStack(stacktrace);
            }
        });
    }

    private static boolean shouldSkipByFilter(List<PreStackFilter> preStackFilters,
                                              IMemberAccessor<IQuantity, IItem> startTimeAccessor,
                                              IMemberAccessor<IMCThread, IItem> threadAccessor,
                                              IItem event) {
        for (PreStackFilter preStackFilter : preStackFilters) {
            if (!preStackFilter.shouldInclude(startTimeAccessor, threadAccessor, event)) {
                return false;
            }
        }
        return false;
    }

    private static EventArrays getFlightRecording(Path file) throws IOException, CouldNotLoadRecordingException {
        if (file.getFileName().toString().toLowerCase().endsWith(".jfr.gz")) {
            return FlightRecordingLoader.loadStream(new GZIPInputStream(Files.newInputStream(file)), false, false);
        }
        return FlightRecordingLoader.loadStream(Files.newInputStream(file), false, false);
    }
}
