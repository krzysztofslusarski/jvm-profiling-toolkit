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
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.Getter;

/**
 * A single node of the flame graph tree, i.e. one method in one call path.
 */
@Getter
public class FlameNode {
    private static final Comparator<FlameNode> BY_NAME = Comparator.comparing(FlameNode::getName);

    private final String name;
    private final FrameType type;
    private final FlameNode parent;
    private final int level;

    private long total;
    private long self;

    private Map<String, FlameNode> childrenByName;
    private List<FlameNode> children = Collections.emptyList();

    FlameNode(String name, FrameType type, FlameNode parent) {
        this.name = name;
        this.type = type;
        this.parent = parent;
        this.level = parent == null ? 0 : parent.level + 1;
    }

    FlameNode child(String rawName) {
        if (childrenByName == null) {
            childrenByName = new HashMap<>();
        }
        return childrenByName.computeIfAbsent(rawName,
                raw -> new FlameNode(FrameType.stripTypeSuffix(raw), FrameType.detect(raw), this));
    }

    void addTotal(long count) {
        total += count;
    }

    void addSelf(long count) {
        self += count;
    }

    void freeze() {
        if (childrenByName == null) {
            children = Collections.emptyList();
            return;
        }
        children = new ArrayList<>(childrenByName.values());
        children.sort(BY_NAME);
        childrenByName = null;
        for (FlameNode child : children) {
            child.freeze();
        }
    }

    public boolean isRoot() {
        return parent == null;
    }

    public FlameNode widestChild() {
        FlameNode widest = null;
        for (FlameNode child : children) {
            if (widest == null || child.total > widest.total) {
                widest = child;
            }
        }
        return widest;
    }

    public FlameNode sibling(int offset) {
        if (parent == null) {
            return null;
        }
        List<FlameNode> siblings = parent.children;
        int index = siblings.indexOf(this) + offset;
        return index < 0 || index >= siblings.size() ? null : siblings.get(index);
    }
}
