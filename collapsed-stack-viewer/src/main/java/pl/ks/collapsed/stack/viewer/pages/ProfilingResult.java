package pl.ks.collapsed.stack.viewer.pages;

import lombok.Builder;
import lombok.Getter;

import javax.validation.Valid;
import java.util.List;

@Valid
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
