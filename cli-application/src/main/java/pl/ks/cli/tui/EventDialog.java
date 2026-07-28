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

import com.googlecode.lanterna.gui2.Component;
import com.googlecode.lanterna.gui2.RadioBoxList;
import java.util.List;
import pl.ks.cli.state.EventType;
import pl.ks.cli.state.ViewerState;

/**
 * Chooses which JFR event feeds the pages, opened with the {@code e} shortcut. Only events present in the parsed files
 * are offered.
 */
class EventDialog extends OkCancelDialog {
    private final ViewerState state;
    private final List<EventType> available;

    private RadioBoxList<String> list;

    EventDialog(ViewerState state, List<EventType> available) {
        super("Event");
        this.state = state;
        this.available = available;
    }

    @Override
    Component createContent() {
        list = new RadioBoxList<>();
        for (EventType eventType : available) {
            list.addItem(eventType.getTitle());
        }
        list.setCheckedItemIndex(Math.max(0, available.indexOf(state.getEventType())));
        return list;
    }

    @Override
    boolean apply() {
        int index = list.getCheckedItemIndex();
        if (index >= 0) {
            state.setEventType(available.get(index));
        }
        return true;
    }
}
