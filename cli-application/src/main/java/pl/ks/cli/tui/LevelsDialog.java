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

import com.googlecode.lanterna.gui2.CheckBox;
import com.googlecode.lanterna.gui2.Component;
import com.googlecode.lanterna.gui2.Direction;
import com.googlecode.lanterna.gui2.Label;
import com.googlecode.lanterna.gui2.LinearLayout;
import com.googlecode.lanterna.gui2.Panel;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import pl.ks.cli.state.ViewerState;
import pl.ks.jfr.parser.tuning.AdditionalLevel;

/**
 * Extra levels prepended to every stack trace of the flame graph, opened with the {@code l} shortcut.
 */
class LevelsDialog extends OkCancelDialog {
    private static final Map<AdditionalLevel, String> LABELS = new EnumMap<>(Map.of(
            AdditionalLevel.THREAD, "Thread",
            AdditionalLevel.TIMESTAMP_10_S, "Timestamp (10s)",
            AdditionalLevel.TIMESTAMP_1_S, "Timestamp (1s)",
            AdditionalLevel.TIMESTAMP_100_MS, "Timestamp (100ms)",
            AdditionalLevel.FILENAME, "Filename",
            AdditionalLevel.SPANS, "Spans",
            AdditionalLevel.LINE_NUMBERS, "Line numbers"
    ));

    private final ViewerState state;
    private final Map<AdditionalLevel, CheckBox> checkBoxes = new EnumMap<>(AdditionalLevel.class);

    LevelsDialog(ViewerState state) {
        super("Additional levels");
        this.state = state;
    }

    @Override
    Component createContent() {
        Panel panel = new Panel(new LinearLayout(Direction.VERTICAL));
        panel.addComponent(new Label("Levels added on top of every stack trace."));
        for (AdditionalLevel level : AdditionalLevel.values()) {
            CheckBox checkBox = new CheckBox(LABELS.get(level)).setChecked(state.getAdditionalLevels().contains(level));
            checkBoxes.put(level, checkBox);
            panel.addComponent(checkBox);
        }
        return panel;
    }

    @Override
    boolean apply() {
        EnumSet<AdditionalLevel> levels = EnumSet.noneOf(AdditionalLevel.class);
        checkBoxes.forEach((level, checkBox) -> {
            if (checkBox.isChecked()) {
                levels.add(level);
            }
        });
        state.setAdditionalLevels(levels);
        return true;
    }
}
