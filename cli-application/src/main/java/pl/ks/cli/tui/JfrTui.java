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

import com.googlecode.lanterna.TerminalSize;
import com.googlecode.lanterna.TextColor;
import com.googlecode.lanterna.gui2.BasicWindow;
import com.googlecode.lanterna.gui2.BorderLayout;
import com.googlecode.lanterna.gui2.Component;
import com.googlecode.lanterna.gui2.DefaultWindowManager;
import com.googlecode.lanterna.gui2.Direction;
import com.googlecode.lanterna.gui2.EmptySpace;
import com.googlecode.lanterna.gui2.Interactable;
import com.googlecode.lanterna.gui2.Label;
import com.googlecode.lanterna.gui2.LinearLayout;
import com.googlecode.lanterna.gui2.MultiWindowTextGUI;
import com.googlecode.lanterna.gui2.Panel;
import com.googlecode.lanterna.gui2.Window;
import com.googlecode.lanterna.gui2.WindowBasedTextGUI;
import com.googlecode.lanterna.gui2.WindowListenerAdapter;
import com.googlecode.lanterna.gui2.dialogs.MessageDialog;
import com.googlecode.lanterna.gui2.dialogs.MessageDialogButton;
import com.googlecode.lanterna.gui2.dialogs.TextInputDialog;
import com.googlecode.lanterna.input.KeyStroke;
import com.googlecode.lanterna.input.KeyType;
import com.googlecode.lanterna.screen.Screen;
import com.googlecode.lanterna.terminal.DefaultTerminalFactory;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import pl.ks.cli.flame.FlameTreeBuilder;
import pl.ks.cli.state.EventType;
import pl.ks.cli.state.PageType;
import pl.ks.cli.state.ViewerState;
import pl.ks.collapsed.CollapsedStack;
import pl.ks.jfr.parser.JfrParsedFile;
import pl.ks.jfr.parser.JfrSpanInfo;
import pl.ks.viewer.JfrViewerFilterAndLevelConfig;
import pl.ks.viewer.StatefulJfrViewerService;
import pl.ks.viewer.TimeTable;

/**
 * Full screen console viewer. The four pages take the whole terminal, every option lives behind a shortcut so that no
 * screen estate is wasted on controls.
 */
public class JfrTui {
    private static final String KEY_HINTS =
            "1-4 pages  e event  f filters  l levels  o options  / search  r reload  x export  ? help  q quit";
    private static final TextColor HEADER_BACKGROUND = TextColor.Indexed.fromRGB(0x00, 0x3a, 0x5f);
    private static final TextColor FOOTER_BACKGROUND = TextColor.Indexed.fromRGB(0x30, 0x30, 0x30);
    private static final DateTimeFormatter FILE_TIMESTAMP = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");

    private final StatefulJfrViewerService service;
    private final UUID fileId;
    private final ViewerState state;
    private final List<EventType> availableEvents;

    private Screen screen;
    private WindowBasedTextGUI gui;
    private BasicWindow window;
    private Label titleLabel;
    private Label detailsLabel;
    private Label footerLabel;
    private Panel content;
    private FlameGraphView flameGraphView;
    private TableView tableView;

    public JfrTui(StatefulJfrViewerService service, UUID fileId, ViewerState state) {
        this.service = service;
        this.fileId = fileId;
        this.state = state;
        this.availableEvents = availableEvents(service.getFile(fileId));
        if (availableEvents.isEmpty()) {
            throw new IllegalArgumentException("The given JFR files contain no execution, wall-clock, allocation or lock samples");
        }
        if (state.getEventType() == null || !availableEvents.contains(state.getEventType())) {
            state.setEventType(availableEvents.get(0));
        }
    }

    private static List<EventType> availableEvents(JfrParsedFile file) {
        List<EventType> available = new ArrayList<>();
        for (EventType eventType : EventType.values()) {
            if (eventType.isAvailableIn(file)) {
                available.add(eventType);
            }
        }
        return available;
    }

    public void run() throws IOException {
        run(new DefaultTerminalFactory().createScreen());
    }

    void run(Screen screen) throws IOException {
        this.screen = screen;
        screen.startScreen();
        screen.setCursorPosition(null);
        try {
            gui = new MultiWindowTextGUI(screen, new DefaultWindowManager(), new EmptySpace(TextColor.ANSI.BLACK));
            buildWindow();
            gui.addWindow(window);
            refresh();
            gui.waitForWindowToClose(window);
        } finally {
            screen.stopScreen();
        }
    }

    private void buildWindow() {
        window = new BasicWindow();
        window.setHints(Set.of(Window.Hint.FULL_SCREEN, Window.Hint.NO_DECORATIONS, Window.Hint.NO_POST_RENDERING));

        titleLabel = new Label("");
        titleLabel.setBackgroundColor(HEADER_BACKGROUND);
        titleLabel.setForegroundColor(TextColor.ANSI.WHITE_BRIGHT);
        detailsLabel = new Label("");
        detailsLabel.setBackgroundColor(HEADER_BACKGROUND);
        detailsLabel.setForegroundColor(TextColor.ANSI.WHITE);
        footerLabel = new Label("");
        footerLabel.setBackgroundColor(FOOTER_BACKGROUND);
        footerLabel.setForegroundColor(TextColor.ANSI.WHITE);

        Panel header = new Panel(new LinearLayout(Direction.VERTICAL).setSpacing(0));
        header.addComponent(titleLabel);
        header.addComponent(detailsLabel);

        content = new Panel(new BorderLayout());
        flameGraphView = new FlameGraphView(state.getEventType().getUnit());
        tableView = new TableView();

        Panel root = new Panel(new BorderLayout());
        root.addComponent(header, BorderLayout.Location.TOP);
        root.addComponent(content, BorderLayout.Location.CENTER);
        root.addComponent(footerLabel, BorderLayout.Location.BOTTOM);
        window.setComponent(root);

        window.addWindowListener(new WindowListenerAdapter() {
            @Override
            public void onUnhandledInput(Window basePane, KeyStroke keyStroke, AtomicBoolean hasBeenHandled) {
                hasBeenHandled.set(handleGlobalKey(keyStroke));
            }

            @Override
            public void onResized(Window window, TerminalSize oldSize, TerminalSize newSize) {
                updateLabels();
            }
        });
    }

    private boolean handleGlobalKey(KeyStroke keyStroke) {
        if (keyStroke.getKeyType() == KeyType.F1) {
            showHelp();
            return true;
        }
        if (keyStroke.getKeyType() == KeyType.F5) {
            refresh();
            return true;
        }
        if (keyStroke.getKeyType() != KeyType.Character) {
            return false;
        }
        switch (Character.toLowerCase(keyStroke.getCharacter())) {
            case '1' -> switchPage(PageType.FLAME_GRAPH);
            case '2' -> switchPage(PageType.TOTAL_TIME);
            case '3' -> switchPage(PageType.SELF_TIME);
            case '4' -> switchPage(PageType.SPAN_STATS);
            case 'e' -> {
                if (new EventDialog(state, availableEvents).showDialog(gui)) {
                    flameGraphView = new FlameGraphView(state.getEventType().getUnit());
                    refresh();
                }
            }
            case 'f' -> {
                if (new FiltersDialog(state).showDialog(gui)) {
                    refresh();
                }
            }
            case 'l' -> {
                if (new LevelsDialog(state).showDialog(gui)) {
                    refresh();
                }
            }
            case 'o' -> {
                if (new OptionsDialog(state).showDialog(gui)) {
                    refresh();
                }
            }
            case 'r' -> refresh();
            case '/' -> search();
            case 'x' -> exportFlameGraph();
            case '?' -> showHelp();
            case 'q' -> window.close();
            default -> {
                return false;
            }
        }
        return true;
    }

    private void switchPage(PageType page) {
        if (state.getPage() != page) {
            state.setPage(page);
            refresh();
        }
    }

    private void search() {
        boolean flameGraph = state.getPage() == PageType.FLAME_GRAPH;
        String description = flameGraph
                ? "Frames containing this text are highlighted."
                : "Only rows containing this text are shown.";
        String pattern = TextInputDialog.showDialog(gui, "Search", description, "");
        if (pattern == null) {
            return;
        }
        if (flameGraph) {
            flameGraphView.setHighlight(pattern);
        } else {
            tableView.setFilter(pattern);
        }
    }

    private void refresh() {
        BasicWindow waiting = new BasicWindow();
        waiting.setHints(Set.of(Window.Hint.CENTERED, Window.Hint.MODAL));
        waiting.setComponent(new Label(" Computing... "));
        gui.addWindow(waiting);
        try {
            gui.updateScreen();
            reloadPage();
        } catch (Exception e) {
            MessageDialog.showMessageDialog(gui, "Error",
                    e.getClass().getSimpleName() + ": " + e.getMessage(), MessageDialogButton.OK);
        } finally {
            gui.removeWindow(waiting);
        }
        updateLabels();
    }

    private void reloadPage() {
        JfrViewerFilterAndLevelConfig config = state.toConfig();
        EventType eventType = state.getEventType();
        switch (state.getPage()) {
            case FLAME_GRAPH -> {
                CollapsedStack collapsed = eventType.collapsed(service, fileId, config);
                // Same as the HTML flame graphs: normal stacks grow upwards, reversed ones hang down as an icicle.
                flameGraphView.setRoot(FlameTreeBuilder.build(collapsed, config.isReverseOn()), !config.isReverseOn());
                showPage(flameGraphView);
            }
            case TOTAL_TIME -> showTimeTable(eventType.timeStats(service, fileId, config, TimeTable.Type.TOTAL_TIME), eventType);
            case SELF_TIME -> showTimeTable(eventType.timeStats(service, fileId, config, TimeTable.Type.SELF_TIME), eventType);
            case SPAN_STATS -> showSpanStats(service.getSpanStats(fileId, config));
        }
    }

    private void showTimeTable(TimeTable table, EventType eventType) {
        List<String[]> rows = new ArrayList<>(table.getRows().size());
        for (TimeTable.Row row : table.getRows()) {
            rows.add(new String[]{row.getMethodName(), Formats.number(row.getSamples()), row.getPercent() + "%"});
        }
        tableView.setData(List.of(
                new TableView.Column("Method name", 0, false),
                new TableView.Column(eventType.getUnit(), 16, true),
                new TableView.Column("Percent", 9, true)
        ), rows);
        showPage(tableView);
    }

    private void showSpanStats(List<JfrSpanInfo> spans) {
        List<String[]> rows = new ArrayList<>(spans.size());
        for (JfrSpanInfo span : spans) {
            rows.add(new String[]{
                    span.getTag(),
                    span.getDurationInMs().toPlainString(),
                    span.getEventTime().toString(),
                    span.getThreadName(),
                    span.getFilename()
            });
        }
        tableView.setData(List.of(
                new TableView.Column("Tag", 0, false),
                new TableView.Column("Duration [ms]", 15, true),
                new TableView.Column("Start time (UTC)", 26, false),
                new TableView.Column("Thread", 24, false),
                new TableView.Column("File", 20, false)
        ), rows);
        showPage(tableView);
    }

    private void showPage(Component component) {
        content.removeAllComponents();
        content.addComponent(component, BorderLayout.Location.CENTER);
        ((Interactable) component).takeFocus();
    }

    private void updateLabels() {
        int width = screen.getTerminalSize().getColumns();
        titleLabel.setText(Formats.pad("JFR viewer - " + state.getPage().getTitle()
                + " | event: " + state.getEventType().getTitle()
                + " | files: " + String.join(", ", service.getFile(fileId).getFilenames()), width, false));
        detailsLabel.setText(Formats.pad("filters: " + state.describeFilters()
                + " | levels: " + state.describeLevels()
                + " | limit: " + state.getTableLimit()
                + " | reverse: " + (state.isReverseOn() ? "on" : "off"), width, false));
        footerLabel.setText(Formats.pad(KEY_HINTS, width, false));
    }

    private void exportFlameGraph() {
        String defaultName = "flame-graph-" + state.getEventType().name().toLowerCase()
                + "-" + LocalDateTime.now().format(FILE_TIMESTAMP) + ".html";
        String fileName = TextInputDialog.showDialog(gui, "Export flame graph",
                "Writes the interactive HTML flame graph of the current event and filters.", defaultName);
        if (fileName == null || fileName.isBlank()) {
            return;
        }
        try {
            Path target = Path.of(fileName.trim()).toAbsolutePath();
            Files.write(target, state.getEventType().flameGraphHtml(service, fileId, state.toConfig()));
            MessageDialog.showMessageDialog(gui, "Export", "Saved to " + target, MessageDialogButton.OK);
        } catch (Exception e) {
            MessageDialog.showMessageDialog(gui, "Export failed",
                    e.getClass().getSimpleName() + ": " + e.getMessage(), MessageDialogButton.OK);
        }
    }

    private void showHelp() {
        MessageDialog.showMessageDialog(gui, "Shortcuts", """
                Pages          1 flame graph   2 total time   3 self time   4 span stats

                Configuration  e  event: execution, wall-clock, allocation, lock
                               f  filters: thread, stack trace, span, time, consumes CPU
                               l  additional levels of the flame graph
                               o  options: table limit, reverse flame graph
                               r  reload the page with the current settings (also F5)

                Flame graph    arrows     move between frames
                               Enter      zoom into the selected frame
                               Backspace  zoom out one level, Home resets the zoom
                               PgUp/PgDn  scroll deeper or shallower
                               /          highlight the frames matching a text

                Tables         arrows     move between rows
                               PgUp/PgDn  page through the rows, Home/End first or last
                               /          show only the rows matching a text (empty clears)

                Other          x  export the current flame graph as an HTML file
                               ?  this help (also F1)
                               q  quit

                Frame colours  green Java compiled, cyan inlined, yellow C++ of the JVM,
                               red native, orange kernel, magenta matches the search""",
                MessageDialogButton.OK);
    }
}
