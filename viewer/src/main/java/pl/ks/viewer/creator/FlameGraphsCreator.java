/*
 * Copyright 2022 Krzysztof Slusarski
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package pl.ks.viewer.creator;

import static pl.ks.viewer.pages.TableWithLinks.Link.of;

import java.util.List;
import lombok.RequiredArgsConstructor;
import pl.ks.viewer.pages.Page;
import pl.ks.viewer.pages.PageCreator;
import pl.ks.viewer.pages.TableWithLinks;

@RequiredArgsConstructor
public class FlameGraphsCreator implements PageCreator {
    private final String collapsedStackFile;
    private final String chartName;
    private final String title;

    @Override
    public Page create() {
        return Page.builder()
                .fullName(chartName)
                .menuName(chartName)
                .icon(Page.Icon.STATS)
                .pageContents(List.of(
                        TableWithLinks.builder()
                                .header(List.of("Type"))
                                .table(
                                        List.of(
                                                List.of(of(LinkUtils.getFlameGraphHref(collapsedStackFile, title), "Flame graph")),
                                                List.of(of(LinkUtils.getSkippedFlameGraphHref(collapsedStackFile, title, 1), "Flame graph with skipped 1 level")),
                                                List.of(of(LinkUtils.getSkippedFlameGraphHref(collapsedStackFile, title, 2), "Flame graph with skipped 2 levels")),
                                                List.of(of(LinkUtils.getSkippedFlameGraphHref(collapsedStackFile, title, 3), "Flame graph with skipped 3 levels")),
                                                List.of(of(LinkUtils.getSkippedFlameGraphHref(collapsedStackFile, title, 4), "Flame graph with skipped 4 levels")),
                                                List.of(of(LinkUtils.getHotspotFlameGraphHref(collapsedStackFile, title), "Hotspot flame graph")),
                                                List.of(of(LinkUtils.getHotspotLLimitedFlameGraphHref(collapsedStackFile, title, 10), "Hotspot flame graph - depth = 10")),
                                                List.of(of(LinkUtils.getHotspotLLimitedFlameGraphHref(collapsedStackFile, title, 20), "Hotspot flame graph - depth = 20")),
                                                List.of(of(LinkUtils.getHotspotLLimitedFlameGraphHref(collapsedStackFile, title, 30), "Hotspot flame graph - depth = 30"))
                                        )
                                )
                                .build()
                ))
                .build();
    }
}
