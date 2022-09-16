package pl.ks.collapsed;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

public class CollapsedStackBufferedReader extends BufferedReader {
    private final Iterator<Map.Entry<String, AtomicLong>> iterator;

    public CollapsedStackBufferedReader(CollapsedStack collapsedStack) {
        super(nullReader());
        iterator = collapsedStack.stacks().entrySet().iterator();
    }

    @Override
    public String readLine() throws IOException {
        if (!iterator.hasNext()) {
            return null;
        }

        Map.Entry<String, AtomicLong> next = iterator.next();
        return next.getKey() + " " + next.getValue().get();
    }

    @Override
    public void close() throws IOException {
        super.close();
    }
}
