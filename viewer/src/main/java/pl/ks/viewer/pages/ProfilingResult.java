package pl.ks.viewer.pages;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class ProfilingResult implements PageContent {
    List<ProfilingEntry> profilingEntries;
    Integer filteredColumn;
    String title;
    String info;

    @Override
    public ContentType getType() {
        return ContentType.PROFILING_RESULTS;
    }
}
