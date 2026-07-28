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
package pl.ks.cli.tui;

import com.googlecode.lanterna.SGR;
import com.googlecode.lanterna.TerminalPosition;
import com.googlecode.lanterna.TerminalSize;
import com.googlecode.lanterna.TextColor;
import com.googlecode.lanterna.gui2.AbstractInteractableComponent;
import com.googlecode.lanterna.gui2.Interactable;
import com.googlecode.lanterna.gui2.InteractableRenderer;
import com.googlecode.lanterna.gui2.TextGUIGraphics;
import com.googlecode.lanterna.input.KeyStroke;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import pl.ks.cli.flame.FlameNode;
import pl.ks.cli.flame.FrameType;

/**
 * A flame graph drawn with coloured terminal cells, with the width of a frame proportional to its sample count. Like
 * the HTML flame graphs it grows upwards from the zoom root, and turns into an icicle growing downwards when the stacks
 * are reversed.
 */
public class FlameGraphView extends AbstractInteractableComponent<FlameGraphView> {
    private final String unit;

    private FlameNode root;
    private FlameNode zoomRoot;
    private FlameNode selected;
    private boolean rootAtBottom = true;
    private String highlight;
    private long matchedTotal;
    private int topLevel;

    private int graphHeight = 1;
    private int drawnDepth = 1;

    public FlameGraphView(String unit) {
        this.unit = unit;
    }

    public void setRoot(FlameNode root, boolean rootAtBottom) {
        this.root = root;
        this.zoomRoot = root;
        this.selected = root;
        this.rootAtBottom = rootAtBottom;
        this.topLevel = 0;
        recalculateMatched();
        invalidate();
    }

    public void setHighlight(String highlight) {
        this.highlight = highlight == null || highlight.isBlank() ? null : highlight.toLowerCase(Locale.ROOT);
        recalculateMatched();
        invalidate();
    }

    @Override
    protected InteractableRenderer<FlameGraphView> createDefaultRenderer() {
        return new InteractableRenderer<>() {
            @Override
            public TerminalPosition getCursorLocation(FlameGraphView component) {
                return null;
            }

            @Override
            public TerminalSize getPreferredSize(FlameGraphView component) {
                return new TerminalSize(80, 25);
            }

            @Override
            public void drawComponent(TextGUIGraphics graphics, FlameGraphView component) {
                component.paint(graphics);
            }
        };
    }

    @Override
    protected Interactable.Result handleKeyStroke(KeyStroke keyStroke) {
        if (root == null) {
            return Interactable.Result.UNHANDLED;
        }
        switch (keyStroke.getKeyType()) {
            case ArrowDown -> moveDeeper(!rootAtBottom);
            case ArrowUp -> moveDeeper(rootAtBottom);
            case ArrowLeft -> moveToSibling(-1);
            case ArrowRight -> moveToSibling(1);
            case Enter -> {
                if (!selected.getChildren().isEmpty()) {
                    zoomRoot = selected;
                    topLevel = 0;
                    recalculateMatched();
                }
            }
            case Backspace -> {
                if (zoomRoot.getParent() != null) {
                    zoomRoot = zoomRoot.getParent();
                    selected = zoomRoot;
                    topLevel = 0;
                    recalculateMatched();
                }
            }
            case Home -> {
                zoomRoot = root;
                selected = root;
                topLevel = 0;
                recalculateMatched();
            }
            case PageDown -> {
                scrollLevels(rootAtBottom ? -graphHeight : graphHeight);
                return Interactable.Result.HANDLED;
            }
            case PageUp -> {
                scrollLevels(rootAtBottom ? graphHeight : -graphHeight);
                return Interactable.Result.HANDLED;
            }
            default -> {
                return Interactable.Result.UNHANDLED;
            }
        }
        scrollToSelected();
        invalidate();
        return Interactable.Result.HANDLED;
    }

    private void moveDeeper(boolean deeper) {
        if (deeper) {
            FlameNode child = selected.widestChild();
            if (child != null) {
                selected = child;
            }
        } else if (selected != zoomRoot && selected.getParent() != null) {
            selected = selected.getParent();
        }
    }

    private void scrollLevels(int levels) {
        topLevel = Math.max(0, Math.min(topLevel + levels, Math.max(0, drawnDepth - 1)));
        invalidate();
    }

    private void moveToSibling(int offset) {
        if (selected == zoomRoot) {
            return;
        }
        FlameNode sibling = selected.sibling(offset);
        if (sibling != null) {
            selected = sibling;
        }
    }

    private void scrollToSelected() {
        int relative = selected.getLevel() - zoomRoot.getLevel();
        if (relative < topLevel) {
            topLevel = relative;
        } else if (relative >= topLevel + graphHeight) {
            topLevel = relative - graphHeight + 1;
        }
    }

    private void recalculateMatched() {
        matchedTotal = highlight == null || zoomRoot == null ? 0 : matched(zoomRoot);
    }

    private long matched(FlameNode node) {
        if (matches(node)) {
            return node.getTotal();
        }
        long sum = 0;
        for (FlameNode child : node.getChildren()) {
            sum += matched(child);
        }
        return sum;
    }

    private boolean matches(FlameNode node) {
        return highlight != null && node.getName().toLowerCase(Locale.ROOT).contains(highlight);
    }

    private void paint(TextGUIGraphics graphics) {
        TerminalSize size = graphics.getSize();
        int width = size.getColumns();
        int height = size.getRows();
        graphics.clearModifiers();
        graphics.setBackgroundColor(TextColor.ANSI.BLACK);
        graphics.setForegroundColor(TextColor.ANSI.WHITE);
        graphics.fill(' ');
        if (width < 4 || height < 2) {
            return;
        }
        graphHeight = height - 1;

        if (root == null || root.getTotal() == 0) {
            graphics.putString(1, 0, "No samples matching the current filters.");
            return;
        }

        List<List<Placed>> levels = new ArrayList<>();
        place(zoomRoot, 0, 0.0, width, levels);
        drawnDepth = levels.size();
        if (topLevel >= drawnDepth) {
            topLevel = Math.max(0, drawnDepth - 1);
        }

        for (int row = 0; row < graphHeight; row++) {
            int level = topLevel + (rootAtBottom ? graphHeight - 1 - row : row);
            if (level >= levels.size()) {
                continue;
            }
            for (Placed placed : levels.get(level)) {
                drawFrame(graphics, placed, row);
            }
        }
        drawDetails(graphics, width, height - 1);
    }

    private void place(FlameNode node, int level, double x, double width, List<List<Placed>> levels) {
        int start = (int) Math.round(x);
        int end = (int) Math.round(x + width);
        if (end <= start || node.getTotal() <= 0) {
            return;
        }
        while (levels.size() <= level) {
            levels.add(new ArrayList<>());
        }
        levels.get(level).add(new Placed(node, start, end - start));

        double childX = x + width * node.getSelf() / node.getTotal();
        for (FlameNode child : node.getChildren()) {
            double childWidth = width * child.getTotal() / node.getTotal();
            place(child, level + 1, childX, childWidth, levels);
            childX += childWidth;
        }
    }

    private void drawFrame(TextGUIGraphics graphics, Placed placed, int row) {
        FlameNode node = placed.node();
        TextColor color = matches(node) ? FrameType.HIGHLIGHT_COLOR : node.getType().getColor();
        int width = placed.width() >= 2 ? placed.width() - 1 : placed.width();

        graphics.clearModifiers();
        if (node == selected) {
            graphics.setBackgroundColor(TextColor.ANSI.BLACK);
            graphics.setForegroundColor(color);
            graphics.enableModifiers(SGR.BOLD, SGR.UNDERLINE);
        } else {
            graphics.setBackgroundColor(color);
            graphics.setForegroundColor(TextColor.ANSI.BLACK);
        }
        graphics.fillRectangle(new TerminalPosition(placed.x(), row), new TerminalSize(width, 1), ' ');

        int padding = width >= 4 ? 1 : 0;
        String label = label(node.getName(), width - 2 * padding);
        if (!label.isEmpty()) {
            graphics.putString(placed.x() + padding, row, label);
        }
        graphics.clearModifiers();
    }

    private void drawDetails(TextGUIGraphics graphics, int width, int row) {
        graphics.clearModifiers();
        graphics.setBackgroundColor(TextColor.ANSI.BLACK);
        graphics.setForegroundColor(TextColor.ANSI.WHITE);
        graphics.fillRectangle(new TerminalPosition(0, row), new TerminalSize(width, 1), ' ');

        StringBuilder details = new StringBuilder();
        details.append(percent(selected.getTotal(), root.getTotal())).append(" of all");
        if (selected.getParent() != null) {
            details.append(", ").append(percent(selected.getTotal(), selected.getParent().getTotal())).append(" of parent");
        }
        details.append(", ").append(Formats.number(selected.getTotal())).append(' ').append(unit.toLowerCase(Locale.ROOT));
        details.append(", self ").append(percent(selected.getSelf(), root.getTotal()));
        if (highlight != null) {
            details.append(" | matched ").append(percent(matchedTotal, zoomRoot.getTotal()));
        }
        if (zoomRoot != root) {
            details.append(" | zoomed");
        }

        String suffix = " [" + details + "]";
        String name = label(selected.getName(), Math.max(0, width - suffix.length()));
        graphics.enableModifiers(SGR.BOLD);
        graphics.putString(0, row, name);
        graphics.clearModifiers();
        graphics.putString(Math.min(name.length(), width), row, label(suffix, width - name.length()));
    }

    private static String percent(long value, long total) {
        if (total <= 0) {
            return "0.00%";
        }
        return String.format(Locale.US, "%.2f%%", value * 100.0 / total);
    }

    /**
     * Frames are usually too narrow for a fully qualified name - drop the package first, then cut the tail.
     */
    private static String label(String name, int width) {
        if (width <= 0) {
            return "";
        }
        if (name.length() <= width) {
            return name;
        }
        String shortName = name;
        int lastSlash = name.lastIndexOf('/');
        if (lastSlash >= 0 && lastSlash + 1 < name.length()) {
            shortName = name.substring(lastSlash + 1);
        }
        if (shortName.length() <= width) {
            return shortName;
        }
        return width <= 2 ? shortName.substring(0, width) : shortName.substring(0, width - 2) + "..";
    }

    private record Placed(FlameNode node, int x, int width) {
    }
}
