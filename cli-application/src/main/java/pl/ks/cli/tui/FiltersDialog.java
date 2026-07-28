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
import com.googlecode.lanterna.gui2.CheckBox;
import com.googlecode.lanterna.gui2.Component;
import com.googlecode.lanterna.gui2.Direction;
import com.googlecode.lanterna.gui2.GridLayout;
import com.googlecode.lanterna.gui2.Label;
import com.googlecode.lanterna.gui2.LinearLayout;
import com.googlecode.lanterna.gui2.Panel;
import com.googlecode.lanterna.gui2.TextBox;
import com.googlecode.lanterna.gui2.dialogs.MessageDialog;
import com.googlecode.lanterna.gui2.dialogs.MessageDialogButton;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import pl.ks.cli.state.ViewerState;

/**
 * All filters of the web viewer sidebar on a single screen, opened with the {@code f} shortcut.
 */
class FiltersDialog extends OkCancelDialog {
    private static final Pattern DIGITS = Pattern.compile("[0-9]*");
    private static final int FIELD_WIDTH = 34;

    private final ViewerState state;

    private CheckBox threadFilterOn;
    private TextBox threadFilter;
    private CheckBox threadFilterContainsOn;
    private TextBox threadFilterContains;
    private CheckBox stackTraceFilterOn;
    private TextBox stackTraceFilters;
    private CheckBox stackTraceNotContainsFilterOn;
    private TextBox stackTraceNotContainsFilters;
    private CheckBox spanFilterEqualsOn;
    private TextBox spanFilterEquals;
    private CheckBox spanFilterContainsOn;
    private TextBox spanFilterContains;
    private CheckBox endDurationOn;
    private TextBox endDate;
    private TextBox endDateDateTimeFormat;
    private TextBox duration;
    private TextBox localeLanguage;
    private CheckBox startEndTimestampOn;
    private TextBox startTs;
    private TextBox endTs;
    private CheckBox warmupCooldownOn;
    private TextBox warmup;
    private TextBox cooldown;
    private CheckBox warmupDurationOn;
    private TextBox wdWarmup;
    private TextBox wdDuration;
    private CheckBox consumeCpuOn;

    private boolean updatingTimeFilters;

    FiltersDialog(ViewerState state) {
        super("Filters (Tab moves between fields, Space toggles)");
        this.state = state;
    }

    @Override
    Component createContent() {
        Panel panel = new Panel(new GridLayout(2));

        section(panel, "Thread");
        threadFilterOn = row(panel, "equals", state.isThreadFilterOn());
        threadFilter = singleLine(state.getThreadFilter());
        panel.addComponent(threadFilter);
        threadFilterContainsOn = row(panel, "contains", state.isThreadFilterContainsOn());
        threadFilterContains = singleLine(state.getThreadFilterContains());
        panel.addComponent(threadFilterContains);

        section(panel, "Stack trace (one value per line, all of them have to match)");
        stackTraceFilterOn = row(panel, "contains", state.isStackTraceFilterOn());
        stackTraceFilters = multiLine(state.getStackTraceFilters());
        panel.addComponent(stackTraceFilters);
        stackTraceNotContainsFilterOn = row(panel, "not contains", state.isStackTraceNotContainsFilterOn());
        stackTraceNotContainsFilters = multiLine(state.getStackTraceNotContainsFilters());
        panel.addComponent(stackTraceNotContainsFilters);

        section(panel, "Span (contains matches any of the lines)");
        spanFilterEqualsOn = row(panel, "equals", state.isSpanFilterEqualsOn());
        spanFilterEquals = singleLine(state.getSpanFilterEquals());
        panel.addComponent(spanFilterEquals);
        spanFilterContainsOn = row(panel, "contains", state.isSpanFilterContainsOn());
        spanFilterContains = multiLine(state.getSpanFilterContains());
        panel.addComponent(spanFilterContains);

        section(panel, "Time (only one of them can be used at a time)");
        endDurationOn = row(panel, "access log", state.isEndDurationOn());
        endDate = singleLine(state.getEndDate());
        endDateDateTimeFormat = singleLine(state.getEndDateDateTimeFormat());
        duration = numeric(state.getDuration());
        localeLanguage = singleLine(state.getLocaleLanguage());
        panel.addComponent(fields(
                "end date", endDate,
                "date format", endDateDateTimeFormat,
                "duration [ms]", duration,
                "locale", localeLanguage));

        startEndTimestampOn = row(panel, "timestamps", state.isStartEndTimestampOn());
        startTs = numeric(state.getStartTs());
        endTs = numeric(state.getEndTs());
        panel.addComponent(fields("start [epoch s]", startTs, "end [epoch s]", endTs));

        warmupCooldownOn = row(panel, "warmup/cooldown", state.isWarmupCooldownOn());
        warmup = numeric(state.getWarmup());
        cooldown = numeric(state.getCooldown());
        panel.addComponent(fields("warmup [s]", warmup, "cooldown [s]", cooldown));

        warmupDurationOn = row(panel, "warmup/duration", state.isWarmupDurationOn());
        wdWarmup = numeric(state.getWdWarmup());
        wdDuration = numeric(state.getWdDuration());
        panel.addComponent(fields("warmup [s]", wdWarmup, "duration [s]", wdDuration));

        makeMutuallyExclusive(endDurationOn, startEndTimestampOn, warmupCooldownOn, warmupDurationOn);

        section(panel, "Other");
        consumeCpuOn = row(panel, "consumes CPU only", state.isConsumeCpuOn());
        panel.addComponent(new Label("(execution and wall-clock samples)"));

        return panel;
    }

    @Override
    boolean apply() {
        if (endDurationOn.isChecked() && endDate.getText().isBlank()) {
            MessageDialog.showMessageDialog(getTextGUI(), "Invalid filter",
                    "The access log filter needs an end date.", MessageDialogButton.OK);
            return false;
        }

        state.setThreadFilterOn(threadFilterOn.isChecked());
        state.setThreadFilter(threadFilter.getText().trim());
        state.setThreadFilterContainsOn(threadFilterContainsOn.isChecked());
        state.setThreadFilterContains(threadFilterContains.getText().trim());
        state.setStackTraceFilterOn(stackTraceFilterOn.isChecked());
        state.setStackTraceFilters(lines(stackTraceFilters));
        state.setStackTraceNotContainsFilterOn(stackTraceNotContainsFilterOn.isChecked());
        state.setStackTraceNotContainsFilters(lines(stackTraceNotContainsFilters));
        state.setSpanFilterEqualsOn(spanFilterEqualsOn.isChecked());
        state.setSpanFilterEquals(spanFilterEquals.getText().trim());
        state.setSpanFilterContainsOn(spanFilterContainsOn.isChecked());
        state.setSpanFilterContains(lines(spanFilterContains));
        state.setEndDate(endDate.getText().trim());
        state.setEndDateDateTimeFormat(endDateDateTimeFormat.getText().trim());
        state.setDuration(asLong(duration));
        state.setLocaleLanguage(localeLanguage.getText().isBlank() ? "EN" : localeLanguage.getText().trim());
        state.setStartTs(asLong(startTs));
        state.setEndTs(asLong(endTs));
        state.setWarmup((int) asLong(warmup));
        state.setCooldown((int) asLong(cooldown));
        state.setWdWarmup((int) asLong(wdWarmup));
        state.setWdDuration(asLong(wdDuration));
        state.setEndDurationOn(endDurationOn.isChecked());
        state.setStartEndTimestampOn(startEndTimestampOn.isChecked());
        state.setWarmupCooldownOn(warmupCooldownOn.isChecked());
        state.setWarmupDurationOn(warmupDurationOn.isChecked());
        state.setConsumeCpuOn(consumeCpuOn.isChecked());
        return true;
    }

    private void makeMutuallyExclusive(CheckBox... checkBoxes) {
        for (CheckBox checkBox : checkBoxes) {
            checkBox.addListener(checked -> {
                if (!checked || updatingTimeFilters) {
                    return;
                }
                updatingTimeFilters = true;
                for (CheckBox other : checkBoxes) {
                    if (other != checkBox) {
                        other.setChecked(false);
                    }
                }
                updatingTimeFilters = false;
            });
        }
    }

    private static void section(Panel panel, String title) {
        panel.addComponent(new Label(""), GridLayout.createHorizontallyFilledLayoutData(2));
        panel.addComponent(new Label("- " + title + " -"), GridLayout.createHorizontallyFilledLayoutData(2));
    }

    private static CheckBox row(Panel panel, String label, boolean checked) {
        CheckBox checkBox = new CheckBox(label).setChecked(checked);
        panel.addComponent(checkBox);
        return checkBox;
    }

    private static Panel fields(Object... labelsAndBoxes) {
        Panel panel = new Panel(new LinearLayout(Direction.VERTICAL).setSpacing(0));
        for (int i = 0; i < labelsAndBoxes.length; i += 2) {
            Panel line = new Panel(new LinearLayout(Direction.HORIZONTAL));
            line.addComponent(new Label(Formats.pad((String) labelsAndBoxes[i], 15, false)));
            line.addComponent((Component) labelsAndBoxes[i + 1]);
            panel.addComponent(line);
        }
        return panel;
    }

    private static TextBox singleLine(String value) {
        return new TextBox(new TerminalSize(FIELD_WIDTH, 1), value == null ? "" : value, TextBox.Style.SINGLE_LINE);
    }

    private static TextBox multiLine(List<String> values) {
        TextBox box = new TextBox(new TerminalSize(FIELD_WIDTH, 3), String.join("\n", values), TextBox.Style.MULTI_LINE);
        box.setVerticalFocusSwitching(false);
        return box;
    }

    private static TextBox numeric(long value) {
        TextBox box = new TextBox(new TerminalSize(16, 1), value == 0 ? "" : Long.toString(value), TextBox.Style.SINGLE_LINE);
        box.setValidationPattern(DIGITS);
        return box;
    }

    private static List<String> lines(TextBox box) {
        List<String> values = new ArrayList<>();
        for (int i = 0; i < box.getLineCount(); i++) {
            String line = box.getLine(i).trim();
            if (!line.isEmpty()) {
                values.add(line);
            }
        }
        return values;
    }

    private static long asLong(TextBox box) {
        String text = box.getText().trim();
        return text.isEmpty() ? 0 : Long.parseLong(text);
    }
}
