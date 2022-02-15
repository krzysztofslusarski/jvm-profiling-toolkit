package pl.ks.jfr.parser;

import lombok.RequiredArgsConstructor;
import lombok.Value;
import pl.ks.collapsed.CollapsedStack;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Value
public class JfrParsedFile {
    CollapsedStack wall = new CollapsedStack();
    CollapsedStack cpu = new CollapsedStack();
    CollapsedStack allocCount = new CollapsedStack();
    CollapsedStack allocSize = new CollapsedStack();
    CollapsedStack lock = new CollapsedStack();
    CollapsedStack cpuLoadJvmTotal = new CollapsedStack();
    CollapsedStack cpuLoadJvmSystem = new CollapsedStack();
    CollapsedStack cpuLoadJvmUser = new CollapsedStack();
    CollapsedStack cpuLoadMachineTotal = new CollapsedStack();

    Map<String, String> cpuInfo = new ConcurrentHashMap<>();
    Map<String, String> osInfo = new ConcurrentHashMap<>();
    Map<String, String> jvmInfo = new ConcurrentHashMap<>();
    Map<String, String> initialSystemProperties = new ConcurrentHashMap<>();

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
            case CPU_LOAD_JVM_TOTAL:
                return cpuLoadJvmTotal;
            case CPU_LOAD_JVM_SYSTEM:
                return cpuLoadJvmSystem;
            case CPU_LOAD_JVM_USER:
                return cpuLoadJvmUser;
            case CPU_LOAD_MACHINE_TOTAL:
                return cpuLoadMachineTotal;
        }

        throw new RuntimeException("?");
    }

    @RequiredArgsConstructor
    public enum Type {
        WALL("wall"),
        CPU("cpu"),
        ALLOC_COUNT("alloc count"),
        ALLOC_SIZE("alloc size"),
        LOCK("lock"),
        CPU_LOAD_JVM_TOTAL("cpu load - jvm total"),
        CPU_LOAD_JVM_SYSTEM("cpu load - jvm system"),
        CPU_LOAD_JVM_USER("cpu load - jvm user"),
        CPU_LOAD_MACHINE_TOTAL("cpu load - machine total");

        private final String name;
    }
}
