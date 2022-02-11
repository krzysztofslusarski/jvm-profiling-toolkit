package pl.ks.jfr.parser;

import lombok.RequiredArgsConstructor;
import lombok.Value;
import pl.ks.collapsed.CollapsedStack;

@Value
public class JfrParsedFile {
    CollapsedStack wall = new CollapsedStack();
    CollapsedStack cpu = new CollapsedStack();
    CollapsedStack allocCount = new CollapsedStack();
    CollapsedStack allocSize = new CollapsedStack();
    CollapsedStack lock = new CollapsedStack();

    public CollapsedStack get(Type type) {
        switch (type) {
            case WALL:
                return wall;
            case CPU:
                return cpu;
            case ALLOC_COUNT:
                return allocCount;
            case ALLOC_SIZE:
                return allocSize;
            case LOCK:
                return lock;
        }
        throw new RuntimeException("?");
    }

    @RequiredArgsConstructor
    public enum Type {
        WALL("wall"),
        CPU("cpu"),
        ALLOC_COUNT("alloc count"),
        ALLOC_SIZE("alloc size"),
        LOCK("lock");

        private final String name;
    }
}
