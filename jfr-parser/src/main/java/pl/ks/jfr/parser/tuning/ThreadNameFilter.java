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
package pl.ks.jfr.parser.tuning;

import lombok.Builder;
import lombok.Value;
import org.openjdk.jmc.common.IMCThread;
import org.openjdk.jmc.common.item.IItem;
import org.openjdk.jmc.common.item.IMemberAccessor;
import pl.ks.jfr.parser.JfrAccessors;

@Value
@Builder
public class ThreadNameFilter implements PreStackFilter {
    String threadName;

    @Override
    public boolean shouldInclude(JfrAccessors accessors, IItem event) {
        return shouldInclude(accessors.getThreadAccessor(), event);
    }

    private boolean shouldInclude(IMemberAccessor<IMCThread, IItem> threadAccessor, IItem event) {
        if (threadAccessor == null) {
            return true;
        }

        String threadName = threadAccessor.getMember(event).getThreadName().toLowerCase();
        return threadName.equalsIgnoreCase(this.threadName);
    }
}
