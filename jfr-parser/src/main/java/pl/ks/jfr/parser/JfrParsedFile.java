package pl.ks.jfr.parser;

import lombok.RequiredArgsConstructor;
import lombok.Value;
import pl.ks.collapsed.CollapsedStack;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

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
    CollapsedStack cpuLoadMachineTotalMinusJvmTotal = new CollapsedStack();

    Map<String, JfrEcidInfo> ecidInfo = new ConcurrentHashMap<>();

    Map<String, String> cpuInfo = new ConcurrentHashMap<>();
    Map<String, String> osInfo = new ConcurrentHashMap<>();
    Map<String, String> jvmInfo = new ConcurrentHashMap<>();
    Map<String, String> initialSystemProperties = new ConcurrentHashMap<>();

    public List<JfrEcidInfo> sortedEcidInfos(int limit) {
        return ecidInfo.values().stream()
                .sorted(Comparator.comparing(JfrEcidInfo::timeDiff).reversed())
                .limit(limit)
                .collect(Collectors.toList());
    }

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
            case CPU_LOAD_MACHINE_TOTAL_MINUS_JVM_TOTAL:
                return cpuLoadMachineTotalMinusJvmTotal;
        }

        throw new RuntimeException("?");
    }

    @RequiredArgsConstructor
    public enum Type {
        WALL("Wall-clock"),
        CPU("CPU"),
        ALLOC_COUNT("Heap allocation (count)"),
        ALLOC_SIZE("Heap allocation (size)"),
        LOCK("Locks"),
        CPU_LOAD_JVM_TOTAL("CPU load (JVM total)"),
        CPU_LOAD_JVM_SYSTEM("CPU load (JVM system)"),
        CPU_LOAD_JVM_USER("CPU load (JVM user)"),
        CPU_LOAD_MACHINE_TOTAL("CPU load (machine total)"),
        CPU_LOAD_MACHINE_TOTAL_MINUS_JVM_TOTAL("CPU load (machine total - jvm total)");

        private final String name;

        public String getName() {
            return name;
        }
    }
}
