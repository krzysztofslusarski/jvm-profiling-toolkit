package pl.ks.collapsed;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

public class CollapsedStack {
    private final Map<String, AtomicLong> stackWithCount = new ConcurrentHashMap<>();
    private AtomicLong totalCount = new AtomicLong();

    public Map<String, AtomicLong> stacks() {
        return stackWithCount;
    }

    public boolean hasSameSizes(CollapsedStack other) {
        if (other == null) {
            return false;
        }
        return other.totalCount.get() == totalCount.get() && other.stackWithCount.size() == stackWithCount.size();
    }

    public boolean isNotEmpty() {
        return totalCount.get() > 0;
    }

    public void addSingleStack(String stack) {
        add(stack, 1);
    }

    public void add(String stack, long count) {
        stackWithCount.computeIfAbsent(stack, s -> new AtomicLong()).addAndGet(count);
        totalCount.addAndGet(count);
    }

    public static class StackCount {
        private long count;

        public void add(long count) {
            this.count += count;
        }

        public long count() {
            return count;
        }
    }
}
