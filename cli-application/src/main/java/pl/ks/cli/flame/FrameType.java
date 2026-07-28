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
package pl.ks.cli.flame;

import com.googlecode.lanterna.TextColor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Frame kinds recognised by async-profiler, with the colours used by its HTML flame graphs. The RGB values are mapped
 * to the 256 colour palette, so the graph looks the same in terminals without true colour support.
 */
@Getter
@RequiredArgsConstructor
public enum FrameType {
    INTERPRETED(0xb2, 0xe1, 0xb2),
    JIT_COMPILED(0x50, 0xe1, 0x50),
    INLINED(0x50, 0xcc, 0xcc),
    NATIVE(0xe1, 0x5a, 0x5a),
    CPP(0xc8, 0xc8, 0x3c),
    KERNEL(0xe1, 0x7d, 0x00),
    C1_COMPILED(0xcc, 0xe8, 0x80),
    ;

    public static final TextColor HIGHLIGHT_COLOR = TextColor.Indexed.fromRGB(0xee, 0x00, 0xee);

    private final int red;
    private final int green;
    private final int blue;

    private TextColor color;

    public TextColor getColor() {
        if (color == null) {
            color = TextColor.Indexed.fromRGB(red, green, blue);
        }
        return color;
    }

    /**
     * Same detection rules as {@code pl.ks.viewer.flamegraph.FlameGraph}, so the console and the HTML flame graph agree
     * on frame kinds.
     */
    public static FrameType detect(String title) {
        if (hasTypeSuffix(title)) {
            return switch (title.substring(title.length() - 4)) {
                case "_[j]" -> JIT_COMPILED;
                case "_[i]" -> INLINED;
                case "_[k]" -> KERNEL;
                case "_[0]" -> INTERPRETED;
                default -> C1_COMPILED;
            };
        }
        if (title.contains("::") || title.startsWith("-[") || title.startsWith("+[")) {
            return CPP;
        }
        if (title.indexOf('/') > 0 && title.charAt(0) != '['
                || title.indexOf('.') > 0 && Character.isUpperCase(title.charAt(0))) {
            return JIT_COMPILED;
        }
        return NATIVE;
    }

    public static String stripTypeSuffix(String title) {
        return hasTypeSuffix(title) ? title.substring(0, title.length() - 4) : title;
    }

    private static boolean hasTypeSuffix(String title) {
        return title.endsWith("_[j]") || title.endsWith("_[i]") || title.endsWith("_[k]")
                || title.endsWith("_[0]") || title.endsWith("_[1]");
    }
}
