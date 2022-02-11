package pl.ks.collapsed;

import java.util.HashMap;
import java.util.Map;

public class CollapsedStack {
    private final Map<String, StackCount> stackWithCount = new HashMap<>();
    private long totalCount = 0;

    public Map<String, StackCount> stacks() {
        return stackWithCount;
    }

    public boolean hasSameSizes(CollapsedStack other) {
        if (other == null) {
            return false;
        }
        return other.totalCount == totalCount && other.stackWithCount.size() == stackWithCount.size();
    }

    public boolean isNotEmpty() {
        return totalCount > 0;
    }

    public void addSingleStack(String stack) {
        add(stack, 1);
    }

    public void add(String stack, long count) {
        stackWithCount.computeIfAbsent(stack, s -> new StackCount()).add(count);
        totalCount += count;
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
