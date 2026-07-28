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
import com.googlecode.lanterna.gui2.BasicWindow;
import com.googlecode.lanterna.gui2.Button;
import com.googlecode.lanterna.gui2.Component;
import com.googlecode.lanterna.gui2.Direction;
import com.googlecode.lanterna.gui2.EmptySpace;
import com.googlecode.lanterna.gui2.LinearLayout;
import com.googlecode.lanterna.gui2.Panel;
import com.googlecode.lanterna.gui2.WindowBasedTextGUI;
import java.util.Set;

/**
 * Base class for the "hidden behind a shortcut" configuration dialogs: content plus OK/Cancel, closable with Escape.
 */
abstract class OkCancelDialog extends BasicWindow {
    private boolean accepted;

    OkCancelDialog(String title) {
        super(title);
        setHints(Set.of(Hint.CENTERED, Hint.MODAL));
        setCloseWindowWithEscape(true);
    }

    /**
     * @return {@code true} when the user confirmed the dialog, so the caller knows it has to refresh the page
     */
    boolean showDialog(WindowBasedTextGUI gui) {
        Panel root = new Panel(new LinearLayout(Direction.VERTICAL));
        root.addComponent(createContent());
        root.addComponent(new EmptySpace(TerminalSize.ONE));

        Panel buttons = new Panel(new LinearLayout(Direction.HORIZONTAL));
        buttons.addComponent(new Button("OK", () -> {
            if (apply()) {
                accepted = true;
                close();
            }
        }));
        buttons.addComponent(new Button("Cancel", this::close));
        root.addComponent(buttons, LinearLayout.createLayoutData(LinearLayout.Alignment.End));

        setComponent(root);
        gui.addWindowAndWait(this);
        return accepted;
    }

    abstract Component createContent();

    /**
     * @return {@code false} to keep the dialog open, e.g. when the entered values do not make sense
     */
    abstract boolean apply();
}
