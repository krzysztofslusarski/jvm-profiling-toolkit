package pl.ks.collapsed.stack.viewer.pages;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class ProfilingEntry {
    String methodName;
    String percent;
    long samples;
    ProfilingLinks profilingLinks;
}
