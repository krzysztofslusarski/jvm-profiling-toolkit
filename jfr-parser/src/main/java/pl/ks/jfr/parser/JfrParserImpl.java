package pl.ks.jfr.parser;


import lombok.extern.slf4j.Slf4j;
import org.openjdk.jmc.common.IMCFrame;
import org.openjdk.jmc.common.IMCMethod;
import org.openjdk.jmc.common.IMCStackTrace;
import org.openjdk.jmc.common.unit.ITypedQuantity;
import org.openjdk.jmc.common.unit.UnitLookup;
import org.openjdk.jmc.flightrecorder.JfrAttributes;
import org.openjdk.jmc.flightrecorder.internal.EventArray;
import org.openjdk.jmc.flightrecorder.internal.EventArrays;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.StopWatch;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.Path;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static pl.ks.jfr.parser.JfrParserHelper.isAsyncAllocNewTLABEvent;
import static pl.ks.jfr.parser.JfrParserHelper.isAsyncAllocOutsideTLABEvent;
import static pl.ks.jfr.parser.JfrParserHelper.isCpuLoadEvent;
import static pl.ks.jfr.parser.JfrParserHelper.isExecutionSampleEvent;
import static pl.ks.jfr.parser.JfrParserHelper.isLockEvent;
import static pl.ks.jfr.parser.JfrParserHelper.isWallClockSampleEvent;
import static pl.ks.jfr.parser.JfrParserHelper.replaceCharacter;
import static pl.ks.jfr.parser.JftFrameType.*;
import static pl.ks.jfr.parser.ParserUtil.getFlightRecording;

@Slf4j
class JfrParserImpl implements JfrParser {
    private static final Map<Class, Field> FIELD_MAP = new ConcurrentHashMap<>();

    @Override
    public JfrParsedFile parse(List<Path> jfrFiles, boolean oldAsyncProfiler, boolean wallClockExactTime) {
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        JfrParsedFile jfrParsedFile = new JfrParsedFile(oldAsyncProfiler, wallClockExactTime);

        jfrFiles.forEach(path -> parseFile(path, jfrParsedFile));
        jfrParsedFile.calculateAggregatedDates();
        stopWatch.stop();
        log.info("Parsing took: {}ms", stopWatch.getLastTaskTimeMillis());
        return jfrParsedFile;
    }

    @Override
    public JfrParsedFile trim(JfrParsedFile parent, String method, JfrParsedFile.Direction direction) {
        JfrParsedFile child = new JfrParsedFile(parent.isOldAsyncProfiler(), parent.isWallClockExactTime());
        parent.filenames.forEach(child::addFilename);
        parent.wallClockSamples.stream()
                .parallel()
                .forEach(event -> {
                    var trimmedEvent = createTrimmedEvent(event, method, direction);
                    if (trimmedEvent != null) {
                        child.addWallClockSampleEvent(trimmedEvent);
                    }
                });
        parent.executionSamples.stream()
                .parallel()
                .forEach(event -> {
                    var trimmedEvent = createTrimmedEvent(event, method, direction);
                    if (trimmedEvent != null) {
                        child.addExecutionSampleEvent(trimmedEvent);
                    }
                });
        parent.allocationSamples.stream()
                .parallel()
                .forEach(event -> {
                    var trimmedEvent = createTrimmedEvent(event, method, direction);
                    if (trimmedEvent != null) {
                        child.addAllocationSampleEvent(trimmedEvent);
                    }
                });
        parent.lockSamples.stream()
                .parallel()
                .forEach(event -> {
                    var trimmedEvent = createTrimmedEvent(event, method, direction);
                    if (trimmedEvent != null) {
                        child.addLockSampleEvent(trimmedEvent);
                    }
                });
        parent.cpuUsageSamples.stream()
                .parallel()
                .forEach(child::addCpuUsageEvent);
        child.calculateAggregatedDates();
        return child;
    }

    private JfrParsedExecutionSampleEvent createTrimmedEvent(JfrParsedExecutionSampleEvent event, String method, JfrParsedFile.Direction direction) {
        var stackTrace = getTrimmedStackTrace(event, method, direction, null);
        if (stackTrace == null) {
            return null;
        }

        var lineNumbers = event.getLineNumbers();

        switch (direction) {
            case UP -> {
                lineNumbers = Arrays.copyOfRange(lineNumbers,
                        event.getStackTrace().length - stackTrace.length, event.getStackTrace().length);
            }
            case DOWN -> {
                lineNumbers = Arrays.copyOfRange(lineNumbers, 0, stackTrace.length);
            }
        }
        ;

        return JfrParsedExecutionSampleEvent.builder()
                .consumesCpu(event.isConsumesCpu())
                .threadName(event.getThreadName())
                .correlationId(event.getCorrelationId())
                .filename(event.getFilename())
                .eventTime(event.getEventTime())
                .stackTrace(stackTrace)
                .lineNumbers(lineNumbers)
                .samples(event.getSamples())
                .build();
    }

    private JfrParsedAllocationEvent createTrimmedEvent(JfrParsedAllocationEvent event, String method, JfrParsedFile.Direction direction) {
        var stackTrace = getTrimmedStackTrace(event, method, direction, JfrParsedAllocationEvent::getObjectClass);
        if (stackTrace == null) {
            return null;
        }

        return JfrParsedAllocationEvent.builder()
                .threadName(event.getThreadName())
                .correlationId(event.getCorrelationId())
                .filename(event.getFilename())
                .eventTime(event.getEventTime())
                .stackTrace(stackTrace)
                .objectClass(event.getObjectClass())
                .size(event.getSize())
                .outsideTLAB(event.isOutsideTLAB())
                .build();
    }

    private JfrParsedLockEvent createTrimmedEvent(JfrParsedLockEvent event, String method, JfrParsedFile.Direction direction) {
        var stackTrace = getTrimmedStackTrace(event, method, direction, JfrParsedLockEvent::getMonitorClass);
        if (stackTrace == null) {
            return null;
        }

        return JfrParsedLockEvent.builder()
                .threadName(event.getThreadName())
                .correlationId(event.getCorrelationId())
                .filename(event.getFilename())
                .eventTime(event.getEventTime())
                .stackTrace(stackTrace)
                .monitorClass(event.getMonitorClass())
                .duration(event.getDuration())
                .build();
    }

    private <T extends JfrParsedCommonStackTraceEvent> String[] getTrimmedStackTrace(
            T event,
            String method,
            JfrParsedFile.Direction direction,
            Function<T, String> additionalEntryFunction
    ) {
        int pos = findMethodPosition(event, method);
        if (pos == -1) {
            if (additionalEntryFunction != null && additionalEntryFunction.apply(event).equals(method)) {
                switch (direction) {
                    case UP -> {
                        return new String[0];
                    }
                    case DOWN -> {
                        return Arrays.copyOf(event.getStackTrace(), event.getStackTrace().length);
                    }
                }
            }

            return null;
        }

        switch (direction) {
            case UP -> {
                return Arrays.copyOfRange(event.getStackTrace(), pos, event.getStackTrace().length);
            }
            case DOWN -> {
                return Arrays.copyOfRange(event.getStackTrace(), 0, pos + 1);
            }
        }
        throw new IllegalArgumentException();
    }

    private int findMethodPosition(JfrParsedCommonStackTraceEvent event, String method) {
        int i = 0;
        for (i = 0; i < event.getStackTrace().length; i++) {
            String frame = event.getStackTrace()[i];
            if (frame.equals(method)) {
                break;
            }
        }
        if (i == event.getStackTrace().length) {
            return -1;
        }
        return i;
    }

    private static void parseFile(Path file, JfrParsedFile jfrParsedFile) {
        String filename = file.getFileName().toString();

        log.info("Input file: " + filename);
        log.info("Parsing JFR");

        try {
            jfrParsedFile.addFilename(filename);
            EventArrays flightRecording = getFlightRecording(file);

            for (EventArray eventArray : flightRecording.getArrays()) {
                if (isExecutionSampleEvent(eventArray)) {
                    processExecutionSample(jfrParsedFile, eventArray, filename);
                } else if (isWallClockSampleEvent(eventArray)) {
                    processWallClockSample(jfrParsedFile, eventArray, filename);
                } else if (isLockEvent(eventArray)) {
                    processLockEvent(jfrParsedFile, eventArray, filename);
                } else if (isAsyncAllocNewTLABEvent(eventArray)) {
                    processAllocEvent(jfrParsedFile, eventArray, filename, false);
                } else if (isAsyncAllocOutsideTLABEvent(eventArray)) {
                    processAllocEvent(jfrParsedFile, eventArray, filename, true);
                } else if (isCpuLoadEvent(eventArray)) {
                    processCpuEvent(jfrParsedFile, eventArray, filename);
                }
            }

            if (jfrParsedFile.isWallClockExactTime() && !jfrParsedFile.getWallClockSamplesToProcess().isEmpty()) {
                extractExactTime(jfrParsedFile);
                jfrParsedFile.getWallClockSamplesToProcess().clear();
            }
        } catch (Exception e) {
            log.error("Fatal error", e);
            throw new RuntimeException(e);
        }
    }

    private static void extractExactTime(JfrParsedFile jfrParsedFile) {
        List<JfrParsedExecutionSampleEvent> toProcess = jfrParsedFile.getWallClockSamplesToProcess();
        Map<String, List<JfrParsedExecutionSampleEvent>> threadToSamples = toProcess.stream()
                .parallel()
                .collect(Collectors.groupingBy(JfrParsedExecutionSampleEvent::getThreadName));
        List<ArrayList<JfrParsedExecutionSampleEvent>> threadSamples = threadToSamples.entrySet().stream()
                .parallel()
                .map(entry -> new ArrayList<>(entry.getValue()))
                .toList();
        threadSamples.stream()
                .parallel()
                .forEach(samples -> extractExactTimeForThread(jfrParsedFile, samples));
    }

    private static void extractExactTimeForThread(JfrParsedFile jfrParsedFile, List<JfrParsedExecutionSampleEvent> samples) {
        if (samples.isEmpty()) {
            return;
        }
        samples.sort(Comparator.comparing(JfrParsedExecutionSampleEvent::getEventTime));
        JfrParsedExecutionSampleEvent currentSample = samples.get(0);
        for (int i = 1; i < samples.size(); i++) {
            JfrParsedExecutionSampleEvent nextSample = samples.get(i);
            if (currentSample.getSamples() == 1) {
                jfrParsedFile.addProcessedWallClockSampleEvent(currentSample);
                currentSample = nextSample;
                continue;
            }

            long diff = ChronoUnit.MILLIS.between(currentSample.getEventTime(), nextSample.getEventTime());
            diff /= currentSample.getSamples();
            JfrParsedExecutionSampleEvent currentSampleWithOneSample = currentSample.withSamples(1);
            for (int j = 0; j < currentSample.getSamples(); j++) {
                jfrParsedFile.addProcessedWallClockSampleEvent(
                        currentSampleWithOneSample.withEventTime(currentSample.getEventTime().plus(diff * i, ChronoUnit.MILLIS))
                );
            }
            currentSample = nextSample;
        }
        jfrParsedFile.addProcessedWallClockSampleEvent(currentSample);
    }

    private static void processCpuEvent(JfrParsedFile jfrParsedFile, EventArray eventArray, String filename) {
        JfrAccessors accessors = JfrAccessors.builder()
                .startTimeAccessor(JfrAttributes.START_TIME.getAccessor(eventArray.getType()))
                .jvmSystemAccessor(JfrParserHelper.findCpuJvmSystemAccessor(eventArray))
                .jvmUserAccessor(JfrParserHelper.findCpuJvmUserAccessor(eventArray))
                .machineTotalAccessor(JfrParserHelper.findMachineTotalAccessor(eventArray))
                .build();
        Arrays.stream(eventArray.getEvents()).parallel().forEach(event -> {
            ITypedQuantity jvmUser = accessors.getJvmUserAccessor().getMember(event);
            ITypedQuantity jvmSystem = accessors.getJvmSystemAccessor().getMember(event);
            ITypedQuantity machineTotal = accessors.getMachineTotalAccessor().getMember(event);

            jfrParsedFile.addCpuUsageEvent(JfrParsedCpuUsageEvent.builder()
                    .eventTime(new Date(accessors.getStartTimeAccessor().getMember(event).longValue() / 1000000).toInstant())
                    .filename(filename)
                    .machineTotal(BigDecimal.valueOf(machineTotal.doubleValue()).setScale(2, RoundingMode.HALF_EVEN))
                    .jvmUser(BigDecimal.valueOf(jvmUser.doubleValue()).setScale(2, RoundingMode.HALF_EVEN))
                    .jvmSystem(BigDecimal.valueOf(jvmSystem.doubleValue()).setScale(2, RoundingMode.HALF_EVEN))
                    .build());
        });

    }

    private static void processLockEvent(JfrParsedFile jfrParsedFile, EventArray eventArray, String filename) {
        JfrAccessors accessors = JfrAccessors.builder()
                .stackTraceAccessor(JfrAttributes.EVENT_STACKTRACE.getAccessor(eventArray.getType()))
                .threadAccessor(JfrAttributes.EVENT_THREAD.getAccessor(eventArray.getType()))
                .startTimeAccessor(JfrAttributes.START_TIME.getAccessor(eventArray.getType()))
                .monitorClassAccessor(JfrParserHelper.findMonitorClassAccessor(eventArray))
                .lockDurationAccessor(JfrParserHelper.findLockDurationAccessor(eventArray))
                .ecidAccessor(JfrParserHelper.findEcidAccessor(eventArray))
                .build();

        Arrays.stream(eventArray.getEvents()).parallel().forEach(event -> {
            List<? extends IMCFrame> frames = accessors.getStackTraceAccessor().getMember(event).getFrames();
            jfrParsedFile.addLockSampleEvent(JfrParsedLockEvent.builder()
                    .correlationId(accessors.getEcidAccessor() != null ? accessors.getEcidAccessor().getMember(event).longValue() : 0L)
                    .duration(accessors.getLockDurationAccessor() != null ? accessors.getLockDurationAccessor().getMember(event).in(UnitLookup.NANOSECOND).longValue() : 0L)
                    .filename(filename)
                    .threadName(jfrParsedFile.getCanonicalString(accessors.getThreadAccessor().getMember(event).getThreadName()))
                    .eventTime(new Date(accessors.getStartTimeAccessor().getMember(event).longValue() / 1000000).toInstant())
                    .stackTrace(getStackTrace(jfrParsedFile, frames))
                    .lineNumbers(getLineNumbers(jfrParsedFile, frames))
                    .monitorClass(jfrParsedFile.getCanonicalString(accessors.getMonitorClassAccessor().getMember(event).getFullName()))
                    .build()
            );
        });
    }

    private static void processAllocEvent(JfrParsedFile jfrParsedFile, EventArray eventArray, String filename, boolean outsideTLAB) {
        JfrAccessors accessors = JfrAccessors.builder()
                .stackTraceAccessor(JfrAttributes.EVENT_STACKTRACE.getAccessor(eventArray.getType()))
                .threadAccessor(JfrAttributes.EVENT_THREAD.getAccessor(eventArray.getType()))
                .startTimeAccessor(JfrAttributes.START_TIME.getAccessor(eventArray.getType()))
                .allocationSizeAccessor(JfrParserHelper.findAllocSizeAccessor(eventArray))
                .objectClassAccessor(JfrParserHelper.findObjectClassAccessor(eventArray))
                .ecidAccessor(JfrParserHelper.findEcidAccessor(eventArray))
                .build();

        Arrays.stream(eventArray.getEvents()).parallel().forEach(event -> {
            IMCStackTrace stackTrace = accessors.getStackTraceAccessor().getMember(event);
            if (stackTrace == null) {
                return;
            }
            List<? extends IMCFrame> frames = stackTrace.getFrames();
            jfrParsedFile.addAllocationSampleEvent(JfrParsedAllocationEvent.builder()
                    .correlationId(accessors.getEcidAccessor() != null ? accessors.getEcidAccessor().getMember(event).longValue() : 0L)
                    .filename(filename)
                    .threadName(jfrParsedFile.getCanonicalString(accessors.getThreadAccessor().getMember(event).getThreadName()))
                    .eventTime(new Date(accessors.getStartTimeAccessor().getMember(event).longValue() / 1000000).toInstant())
                    .stackTrace(getStackTrace(jfrParsedFile, frames))
                    .lineNumbers(getLineNumbers(jfrParsedFile, frames))
                    .objectClass(jfrParsedFile.getCanonicalString(accessors.getObjectClassAccessor().getMember(event).getFullName()))
                    .size(accessors.getAllocationSizeAccessor().getMember(event).longValue())
                    .outsideTLAB(outsideTLAB)
                    .build()
            );
        });

    }

    private static void processWallClockSample(JfrParsedFile jfrParsedFile, EventArray eventArray, String filename) {
        JfrAccessors accessors = JfrAccessors.builder()
                .stackTraceAccessor(JfrAttributes.EVENT_STACKTRACE.getAccessor(eventArray.getType()))
                .threadAccessor(JfrParserHelper.findThreadAccessor(eventArray))
                .startTimeAccessor(JfrAttributes.START_TIME.getAccessor(eventArray.getType()))
                .stateAccessor(JfrParserHelper.findStateAccessor(eventArray))
                .ecidAccessor(JfrParserHelper.findEcidAccessor(eventArray))
                .samplesAccessor(JfrParserHelper.findSamplesAccessor(eventArray))
                .build();

        Arrays.stream(eventArray.getEvents()).parallel().forEach(event -> {
            IMCStackTrace stackTrace = accessors.getStackTraceAccessor().getMember(event);
            if (stackTrace == null) {
                return;
            }
            List<? extends IMCFrame> frames = stackTrace.getFrames();
            jfrParsedFile.addWallClockSampleEvent(JfrParsedExecutionSampleEvent.builder()
                    .consumesCpu(accessors.getStateAccessor() != null && JfrParserHelper.isConsumingCpu(accessors.getStateAccessor().getMember(event)))
                    .correlationId(accessors.getEcidAccessor() != null ? accessors.getEcidAccessor().getMember(event).longValue() : 0L)
                    .filename(filename)
                    .threadName(jfrParsedFile.getCanonicalString(accessors.getThreadAccessor().getMember(event).getThreadName()))
                    .eventTime(new Date(accessors.getStartTimeAccessor().getMember(event).longValue() / 1000000).toInstant())
                    .stackTrace(getStackTrace(jfrParsedFile, frames))
                    .lineNumbers(getLineNumbers(jfrParsedFile, frames))
                    .samples(accessors.getSamplesAccessor() != null ? accessors.getSamplesAccessor().getMember(event).longValue() : 0L)
                    .build()
            );
        });
    }

    private static void processExecutionSample(JfrParsedFile jfrParsedFile, EventArray eventArray, String filename) {
        JfrAccessors accessors = JfrAccessors.builder()
                .stackTraceAccessor(JfrAttributes.EVENT_STACKTRACE.getAccessor(eventArray.getType()))
                .threadAccessor(JfrAttributes.EVENT_THREAD.getAccessor(eventArray.getType()))
                .startTimeAccessor(JfrAttributes.START_TIME.getAccessor(eventArray.getType()))
                .stateAccessor(JfrParserHelper.findStateAccessor(eventArray))
                .ecidAccessor(JfrParserHelper.findEcidAccessor(eventArray))
                .build();

        Arrays.stream(eventArray.getEvents()).parallel().forEach(event -> {
            List<? extends IMCFrame> frames = accessors.getStackTraceAccessor().getMember(event).getFrames();
            jfrParsedFile.addExecutionSampleEvent(JfrParsedExecutionSampleEvent.builder()
                    .consumesCpu(accessors.getStateAccessor() != null && JfrParserHelper.isConsumingCpu(accessors.getStateAccessor().getMember(event)))
                    .correlationId(accessors.getEcidAccessor() != null ? accessors.getEcidAccessor().getMember(event).longValue() : 0L)
                    .filename(filename)
                    .threadName(jfrParsedFile.getCanonicalString(accessors.getThreadAccessor().getMember(event).getThreadName()))
                    .eventTime(new Date(accessors.getStartTimeAccessor().getMember(event).longValue() / 1000000).toInstant())
                    .stackTrace(getStackTrace(jfrParsedFile, frames))
                    .lineNumbers(getLineNumbers(jfrParsedFile, frames))
                    .samples(1)
                    .build()
            );
        });
    }

    private static int[] getLineNumbers(JfrParsedFile jfrParsedFile, List<? extends IMCFrame> frames) {
        int[] lineNumbers = new int[frames.size()];
        for (int i = 0; i < frames.size(); i++) {
            IMCFrame frame = frames.get(i);
            Integer lineNumber = frame.getFrameLineNumber();
            lineNumbers[frames.size() - i - 1] = lineNumber == null ? -1 : lineNumber;
        }
        return lineNumbers;
    }

    private static String[] getStackTrace(JfrParsedFile jfrParsedFile, List<? extends IMCFrame> frames) {
        String[] stackTrace = new String[frames.size()];
        for (int i = 0; i < frames.size(); i++) {
            StringBuilder stackTraceBuilder = new StringBuilder();
            IMCFrame frame = frames.get(i);

            JftFrameType type = getType(frame);

            IMCMethod method = frame.getMethod();
            String packageName = method.getType().getPackage().getName() == null ? "" : replaceCharacter(method.getType().getPackage().getName(), '/', '.');
            if (packageName.length() > 0) {
                stackTraceBuilder.append(packageName);
                stackTraceBuilder.append("/");
            }
            if (!method.getFormalDescriptor().equals("()L;")) {
                String className = replaceCharacter(method.getType().getTypeName(), '/', '.');
                if (className.length() > 0) {
                    stackTraceBuilder.append(className);
                    stackTraceBuilder.append(".");
                }
            }
            stackTraceBuilder.append(method.getMethodName());
            if (method.getFormalDescriptor().equals("(Lk;)L;")) {
                stackTraceBuilder.append("_[k]");
            } else if (!jfrParsedFile.isOldAsyncProfiler()) {
                switch (type) {
                    case TYPE_INTERPRETED -> {
                        stackTraceBuilder.append("_[0]");
                    }
                    case TYPE_JIT_COMPILED -> {
                        stackTraceBuilder.append("_[j]");
                    }
                    case TYPE_INLINED -> {
                        stackTraceBuilder.append("_[i]");
                    }
                    case TYPE_KERNEL -> {
                        stackTraceBuilder.append("_[k]");
                    }
                    case TYPE_C1_COMPILED -> {
                        stackTraceBuilder.append("_[1]");
                    }
                }
            }
            stackTrace[frames.size() - i - 1] = jfrParsedFile.getCanonicalString(stackTraceBuilder.toString());
        }
        return stackTrace;
    }

    private static JftFrameType getType(IMCFrame frame) {
        Field field = FIELD_MAP.get(frame.getClass());
        if (field == null) {
            synchronized (FIELD_MAP) {
                field = FIELD_MAP.get(frame.getClass());
                if (field == null) {
                    field = ReflectionUtils.findField(frame.getClass(), "type");
                    field.setAccessible(true);
                    FIELD_MAP.put(frame.getClass(), field);
                }
            }
        }
        Object type = ReflectionUtils.getField(field, frame);
        if (type == null) {
            return TYPE_JIT_COMPILED;
        }
        if (type instanceof String) {
            if ("Native".equals(type)) {
                return TYPE_NATIVE;
            } else if ("Kernel".equals(type)) {
                return TYPE_KERNEL;
            } else if ("Inlined".equals(type)) {
                return TYPE_INLINED;
            } else if ("JIT compiled".equals(type)) {
                return TYPE_JIT_COMPILED;
            } else if ("C++".equals(type)) {
                return TYPE_CPP;
            } else if ("C1 compiled".equals(type)) {
                return TYPE_C1_COMPILED;
            } else if ("Interpreted".equals(type)) {
                return TYPE_INTERPRETED;
            }
        }
        return TYPE_JIT_COMPILED;
    }
}
