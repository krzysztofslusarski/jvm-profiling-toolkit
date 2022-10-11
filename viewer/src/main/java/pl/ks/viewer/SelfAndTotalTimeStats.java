package pl.ks.viewer;

import lombok.Value;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Value
public class SelfAndTotalTimeStats {
    AtomicLong totalCounter = new AtomicLong();
    Map<String, SelfAndTotalTimeMethodStats> methodStats = new ConcurrentHashMap<>();

    void newStackTrace(long count) {
        totalCounter.addAndGet(count);
    }

    void methodSample(String name, boolean consumingResource, long count) {
        SelfAndTotalTimeMethodStats stats = methodStats.computeIfAbsent(name, SelfAndTotalTimeMethodStats::new);
        stats.totalTimeSamples.addAndGet(count);
        if (consumingResource) {
            stats.selfTimeSamples.addAndGet(count);
        }
    }

    @Value
    public static class SelfAndTotalTimeMethodStats {
        String name;
        AtomicLong totalTimeSamples = new AtomicLong();
        AtomicLong selfTimeSamples = new AtomicLong();
    }
}
