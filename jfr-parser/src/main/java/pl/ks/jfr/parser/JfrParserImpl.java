package pl.ks.jfr.parser;

import lombok.extern.slf4j.Slf4j;
import org.openjdk.jmc.common.IMCFrame;
import org.openjdk.jmc.common.IMCMethod;
import org.openjdk.jmc.flightrecorder.JfrAttributes;
import org.openjdk.jmc.flightrecorder.internal.EventArray;
import org.openjdk.jmc.flightrecorder.internal.EventArrays;
import org.springframework.util.StopWatch;
import pl.ks.jfr.parser.JfrParsedExecutionSampleEvent.JfrParsedExecutionSampleEventBuilder;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import static pl.ks.jfr.parser.JfrParserHelper.isExecutionSampleEvent;
import static pl.ks.jfr.parser.JfrParserHelper.replaceCharacter;
import static pl.ks.jfr.parser.ParserUtil.getFlightRecording;

@Slf4j
class JfrParserImpl implements JfrParser {
    @Override
    public JfrParsedFile parse(List<Path> jfrFiles) {
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        JfrParsedFile jfrParsedFile = new JfrParsedFile();
        jfrFiles.forEach(path -> parseFile(path, jfrParsedFile));
        stopWatch.stop();
        log.info("Parsing took: {}ms", stopWatch.getLastTaskTimeMillis());
        return jfrParsedFile;
    }

    private static void parseFile(Path file, JfrParsedFile jfrParsedFile) {
        String filename = file.getFileName().toString();

        log.info("Input file: " + filename);
        log.info("Parsing JFR");

        try {
            EventArrays flightRecording = getFlightRecording(file);

            for (EventArray eventArray : flightRecording.getArrays()) {
                if (isExecutionSampleEvent(eventArray)) {
                    processExecutionSample(jfrParsedFile, eventArray, filename);
                }
            }
        } catch (Exception e) {
            log.error("Fatal error", e);
            throw new RuntimeException(e);
        }
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
            JfrParsedExecutionSampleEventBuilder builder = JfrParsedExecutionSampleEvent.builder();

            builder.consumesCpu(accessors.getStateAccessor() != null && JfrParserHelper.isConsumingCpu(accessors.getStateAccessor().getMember(event)))
                    .correlationId(accessors.getEcidAccessor() != null ? accessors.getEcidAccessor().getMember(event).longValue() : 0L)
                    .filename(filename)
                    .threadName(accessors.getThreadAccessor().getMember(event).getThreadName())
                    .eventTime(new Date(accessors.getStartTimeAccessor().getMember(event).longValue() / 1000000).toInstant());

            List<? extends IMCFrame> frames = accessors.getStackTraceAccessor().getMember(event).getFrames();
            String[] stackTrace = getStackTrace(jfrParsedFile, frames);
            builder.stackTrace(stackTrace);
            jfrParsedFile.addExecutionSampleEvent(builder.build());
        });
    }

    private static String[] getStackTrace(JfrParsedFile jfrParsedFile, List<? extends IMCFrame> frames) {
        String[] stackTrace = new String[frames.size()];
        for (int i = 0; i < frames.size(); i++) {
            StringBuilder stackTraceBuilder = new StringBuilder();
            IMCFrame frame = frames.get(i);
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
            }
            String frameStr = stackTraceBuilder.toString();
            String canonicalFrameStr = jfrParsedFile.getCanonicalFrames().putIfAbsent(frameStr, frameStr);
            stackTrace[i] = canonicalFrameStr == null ? frameStr : canonicalFrameStr;
        }
        return stackTrace;
    }

    public static void main(String[] args) throws InterruptedException {
        JfrParsedFile parsedFile = new JfrParserImpl().parse(Collections.singletonList(Path.of("/home/pasq/Hazelcast/ProfilingArchive/11-FirstTimeEcid/10.212.1.101.jfr.gz")));
        Thread.sleep(1_000L*1_000L*1_000L);
    }
}
