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

/**
 * A scrollable, searchable text table - the console counterpart of the total time, self time and span stats pages.
 */
public class TableView extends AbstractInteractableComponent<TableView> {
    private static final TextColor HEADER_BACKGROUND = TextColor.Indexed.fromRGB(0x30, 0x30, 0x30);
    private static final TextColor SELECTION_BACKGROUND = TextColor.Indexed.fromRGB(0x00, 0x5f, 0x87);

    private List<Column> columns = List.of();
    private List<String[]> allRows = List.of();
    private List<String[]> rows = List.of();
    private String filter;
    private int selectedIndex;
    private int scrollOffset;
    private int visibleRows = 1;

    public void setData(List<Column> columns, List<String[]> rows) {
        this.columns = columns;
        this.allRows = rows;
        this.selectedIndex = 0;
        this.scrollOffset = 0;
        applyFilter();
        invalidate();
    }

    public void setFilter(String filter) {
        this.filter = filter == null || filter.isBlank() ? null : filter.toLowerCase(Locale.ROOT);
        this.selectedIndex = 0;
        this.scrollOffset = 0;
        applyFilter();
        invalidate();
    }

    public String[] getSelectedRow() {
        return selectedIndex >= 0 && selectedIndex < rows.size() ? rows.get(selectedIndex) : null;
    }

    private void applyFilter() {
        if (filter == null) {
            rows = allRows;
            return;
        }
        List<String[]> filtered = new ArrayList<>();
        for (String[] row : allRows) {
            for (String cell : row) {
                if (cell != null && cell.toLowerCase(Locale.ROOT).contains(filter)) {
                    filtered.add(row);
                    break;
                }
            }
        }
        rows = filtered;
    }

    @Override
    protected InteractableRenderer<TableView> createDefaultRenderer() {
        return new InteractableRenderer<>() {
            @Override
            public TerminalPosition getCursorLocation(TableView component) {
                return null;
            }

            @Override
            public TerminalSize getPreferredSize(TableView component) {
                return new TerminalSize(80, 25);
            }

            @Override
            public void drawComponent(TextGUIGraphics graphics, TableView component) {
                component.paint(graphics);
            }
        };
    }

    @Override
    protected Interactable.Result handleKeyStroke(KeyStroke keyStroke) {
        int lastIndex = rows.size() - 1;
        switch (keyStroke.getKeyType()) {
            case ArrowDown -> selectedIndex = Math.min(lastIndex, selectedIndex + 1);
            case ArrowUp -> selectedIndex = Math.max(0, selectedIndex - 1);
            case PageDown -> selectedIndex = Math.min(lastIndex, selectedIndex + visibleRows);
            case PageUp -> selectedIndex = Math.max(0, selectedIndex - visibleRows);
            case Home -> selectedIndex = 0;
            case End -> selectedIndex = Math.max(0, lastIndex);
            default -> {
                return Interactable.Result.UNHANDLED;
            }
        }
        if (selectedIndex < scrollOffset) {
            scrollOffset = selectedIndex;
        } else if (selectedIndex >= scrollOffset + visibleRows) {
            scrollOffset = selectedIndex - visibleRows + 1;
        }
        invalidate();
        return Interactable.Result.HANDLED;
    }

    private void paint(TextGUIGraphics graphics) {
        TerminalSize size = graphics.getSize();
        int width = size.getColumns();
        int height = size.getRows();
        graphics.clearModifiers();
        graphics.setBackgroundColor(TextColor.ANSI.BLACK);
        graphics.setForegroundColor(TextColor.ANSI.WHITE);
        graphics.fill(' ');
        if (width < 8 || height < 3 || columns.isEmpty()) {
            return;
        }
        visibleRows = height - 2;

        int[] widths = resolveWidths(width);
        drawHeader(graphics, width, widths);

        if (rows.isEmpty()) {
            graphics.putString(1, 1, "No rows matching the current filters.");
            drawStatus(graphics, width, height - 1);
            return;
        }
        if (selectedIndex >= rows.size()) {
            selectedIndex = rows.size() - 1;
        }
        for (int i = 0; i < visibleRows; i++) {
            int index = scrollOffset + i;
            if (index >= rows.size()) {
                break;
            }
            drawRow(graphics, width, widths, rows.get(index), i + 1, index == selectedIndex);
        }
        drawStatus(graphics, width, height - 1);
    }

    private int[] resolveWidths(int totalWidth) {
        int[] widths = new int[columns.size()];
        int fixed = 0;
        int flexIndex = -1;
        for (int i = 0; i < columns.size(); i++) {
            widths[i] = columns.get(i).width();
            if (widths[i] <= 0) {
                flexIndex = i;
            } else {
                fixed += widths[i];
            }
        }
        int available = totalWidth - fixed - (columns.size() - 1);
        if (flexIndex >= 0) {
            widths[flexIndex] = Math.max(8, available);
        } else if (available > 0) {
            widths[widths.length - 1] += available;
        }
        return widths;
    }

    private void drawHeader(TextGUIGraphics graphics, int width, int[] widths) {
        graphics.setBackgroundColor(HEADER_BACKGROUND);
        graphics.setForegroundColor(TextColor.ANSI.WHITE);
        graphics.enableModifiers(SGR.BOLD);
        graphics.fillRectangle(TerminalPosition.TOP_LEFT_CORNER, new TerminalSize(width, 1), ' ');
        int x = 0;
        for (int i = 0; i < columns.size() && x < width; i++) {
            graphics.putString(x, 0, Formats.pad(columns.get(i).title(), Math.min(widths[i], width - x), columns.get(i).rightAligned()));
            x += widths[i] + 1;
        }
        graphics.clearModifiers();
    }

    private void drawRow(TextGUIGraphics graphics, int width, int[] widths, String[] row, int y, boolean isSelected) {
        graphics.setBackgroundColor(isSelected ? SELECTION_BACKGROUND : TextColor.ANSI.BLACK);
        graphics.setForegroundColor(TextColor.ANSI.WHITE);
        graphics.fillRectangle(new TerminalPosition(0, y), new TerminalSize(width, 1), ' ');
        int x = 0;
        for (int i = 0; i < columns.size() && i < row.length && x < width; i++) {
            String cell = row[i] == null ? "" : row[i];
            graphics.putString(x, y, Formats.pad(cell, Math.min(widths[i], width - x), columns.get(i).rightAligned()));
            x += widths[i] + 1;
        }
    }

    private void drawStatus(TextGUIGraphics graphics, int width, int y) {
        graphics.setBackgroundColor(TextColor.ANSI.BLACK);
        graphics.setForegroundColor(TextColor.ANSI.WHITE_BRIGHT);
        graphics.fillRectangle(new TerminalPosition(0, y), new TerminalSize(width, 1), ' ');

        StringBuilder status = new StringBuilder();
        status.append(rows.isEmpty() ? 0 : selectedIndex + 1).append(" of ").append(Formats.number(rows.size()));
        if (filter != null) {
            status.append(" (search: ").append(filter).append(", ").append(Formats.number(allRows.size())).append(" total)");
        }
        String[] selectedRow = getSelectedRow();
        if (selectedRow != null && selectedRow.length > 0 && selectedRow[0] != null) {
            status.append("  ").append(selectedRow[0]);
        }
        graphics.putString(0, y, Formats.pad(status.toString(), width, false));
    }

    public record Column(String title, int width, boolean rightAligned) {
    }
}
