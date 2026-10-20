package pl.ks.collapsed;

import java.io.Reader;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Streams a {@link CollapsedStack} as collapsed-stack text ("frame;frame;frame count\n" per line)
 * without materializing the whole content in memory.
 */
public class CollapsedStackReader extends Reader {
    private final Iterator<Map.Entry<String, AtomicLong>> iterator;
    private String currentLine = "";
    private int position;

    public CollapsedStackReader(CollapsedStack collapsedStack) {
        iterator = collapsedStack.stacks().entrySet().iterator();
    }

    @Override
    public int read(char[] cbuf, int off, int len) {
        Objects.checkFromIndexSize(off, len, cbuf.length);
        if (len == 0) {
            return 0;
        }

        int read = 0;
        while (read < len) {
            if (position == currentLine.length()) {
                if (!iterator.hasNext()) {
                    break;
                }
                Map.Entry<String, AtomicLong> next = iterator.next();
                currentLine = next.getKey() + " " + next.getValue().get() + "\n";
                position = 0;
            }
            int count = Math.min(len - read, currentLine.length() - position);
            currentLine.getChars(position, position + count, cbuf, off + read);
            position += count;
            read += count;
        }
        return read == 0 ? -1 : read;
    }

    @Override
    public void close() {
    }
}
