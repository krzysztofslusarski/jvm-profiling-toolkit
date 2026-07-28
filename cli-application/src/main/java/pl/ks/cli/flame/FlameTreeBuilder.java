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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import pl.ks.collapsed.CollapsedStack;

/**
 * Turns collapsed stacks into a tree that can be painted in a terminal.
 */
public final class FlameTreeBuilder {
    private FlameTreeBuilder() {
    }

    public static FlameNode build(CollapsedStack collapsed, boolean reversed) {
        FlameNode root = new FlameNode("all", FrameType.NATIVE, null);
        List<String> frames = new ArrayList<>();
        for (Map.Entry<String, AtomicLong> entry : collapsed.stacks().entrySet()) {
            long count = entry.getValue().get();
            if (count <= 0) {
                continue;
            }
            split(entry.getKey(), frames);
            root.addTotal(count);
            FlameNode node = root;
            for (int i = 0; i < frames.size(); i++) {
                node = node.child(frames.get(reversed ? frames.size() - 1 - i : i));
                node.addTotal(count);
            }
            node.addSelf(count);
        }
        root.freeze();
        return root;
    }

    private static void split(String stack, List<String> into) {
        into.clear();
        int from = 0;
        while (from <= stack.length()) {
            int to = stack.indexOf(';', from);
            if (to < 0) {
                to = stack.length();
            }
            if (to > from) {
                into.add(stack.substring(from, to));
            }
            from = to + 1;
        }
    }
}
