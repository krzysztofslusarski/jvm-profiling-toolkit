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
package pl.ks.viewer.pages;

import java.util.List;
import lombok.Builder;
import lombok.Getter;
import lombok.Value;

@Getter
@Value
@Builder
public class TableWithLinks implements PageContent {
    private List<String> header;
    private List<String> footer;
    private List<List<Link>> table;
    private Integer filteredColumn;
    private String title;
    private String screenWidth;
    private String info;

    @Override
    public ContentType getType() {
        return ContentType.TABLE_WITH_LINKS;
    }

    public String getScreenWidth() {
        if (screenWidth == null) {
            return "100%";
        }
        return screenWidth;
    }

    @Builder
    @Getter
    public static class Link {
        private String href;
        private String description;
        private LinkColor linkColor;

        public static Link of(String description) {
            return Link.builder()
                    .description(description)
                    .build();
        }

        public static Link of(String description, LinkColor linkColor) {
            return Link.builder()
                    .description(description)
                    .linkColor(linkColor)
                    .build();
        }

        public static Link of(String href, String description) {
            return Link.builder()
                    .href(href)
                    .description(description)
                    .build();
        }

        public static Link of(String href, String description, LinkColor linkColor) {
            return Link.builder()
                    .href(href)
                    .description(description)
                    .linkColor(linkColor)
                    .build();
        }
    }

    public enum LinkColor {
        RED,
        GREEN,
    }
}
