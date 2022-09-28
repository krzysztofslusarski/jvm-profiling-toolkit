package pl.ks.viewer;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import lombok.Value;

@Value
public class SelfAndTotalTimeStats {
    AtomicLong totalCounter = new AtomicLong();
    Map<String, SelfAndTotalTimeMethodStats> methodStats = new ConcurrentHashMap<>();

    void newStackTrace() {
        totalCounter.incrementAndGet();
    }

    void methodSample(String name, boolean consumingResource) {
        SelfAndTotalTimeMethodStats stats = methodStats.computeIfAbsent(name, SelfAndTotalTimeMethodStats::new);
        stats.totalTimeSamples.incrementAndGet();
        if (consumingResource) {
            stats.selfTimeSamples.incrementAndGet();
        }
    }

    @Value
    public static class SelfAndTotalTimeMethodStats {
        String name;
        AtomicLong totalTimeSamples = new AtomicLong();
        AtomicLong selfTimeSamples = new AtomicLong();
    }
}
