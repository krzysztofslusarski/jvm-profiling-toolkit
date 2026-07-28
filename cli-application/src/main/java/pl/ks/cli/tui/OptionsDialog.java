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
import com.googlecode.lanterna.gui2.Label;
import com.googlecode.lanterna.gui2.LinearLayout;
import com.googlecode.lanterna.gui2.Panel;
import com.googlecode.lanterna.gui2.TextBox;
import java.util.regex.Pattern;
import pl.ks.cli.state.ViewerState;

/**
 * Options shared by all pages, opened with the {@code o} shortcut.
 */
class OptionsDialog extends OkCancelDialog {
    private final ViewerState state;

    private TextBox tableLimit;
    private CheckBox reverseOn;

    OptionsDialog(ViewerState state) {
        super("Options");
        this.state = state;
    }

    @Override
    Component createContent() {
        Panel panel = new Panel(new LinearLayout(Direction.VERTICAL));

        Panel limitLine = new Panel(new LinearLayout(Direction.HORIZONTAL));
        limitLine.addComponent(new Label("Table limit (rows)"));
        tableLimit = new TextBox(new TerminalSize(10, 1), Integer.toString(state.getTableLimit()), TextBox.Style.SINGLE_LINE);
        tableLimit.setValidationPattern(Pattern.compile("[0-9]*"));
        limitLine.addComponent(tableLimit);
        panel.addComponent(limitLine);

        reverseOn = new CheckBox("Reverse flame graph").setChecked(state.isReverseOn());
        panel.addComponent(reverseOn);
        return panel;
    }

    @Override
    boolean apply() {
        String limit = tableLimit.getText().trim();
        state.setTableLimit(limit.isEmpty() ? 0 : Integer.parseInt(limit));
        state.setReverseOn(reverseOn.isChecked());
        return true;
    }
}
