package pl.ks.collapsed.stack.viewer.pages;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class ProfilingLinks {
    String fromMethodFlameGraph;
    String toMethodFlameGraph;
    String fromMethodRoot;
    String toMethodRoot;
}