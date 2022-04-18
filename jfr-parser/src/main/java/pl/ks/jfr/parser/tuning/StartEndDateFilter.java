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

import java.time.Instant;
import lombok.Builder;
import lombok.Value;
import org.openjdk.jmc.common.item.IItem;
import org.openjdk.jmc.common.item.IMemberAccessor;
import org.openjdk.jmc.common.unit.IQuantity;
import pl.ks.jfr.parser.JfrAccessors;

@Value
@Builder
public class StartEndDateFilter implements PreStackFilter{
    Instant startDate;
    Instant endDate;

    @Override
    public boolean shouldInclude(JfrAccessors accessors, IItem event) {
        return shouldInclude(accessors.getStartTimeAccessor(), event);
    }

    private boolean shouldInclude(IMemberAccessor<IQuantity, IItem> startTimeAccessor, IItem event) {
        long startTimestamp = startTimeAccessor.getMember(event).longValue();
        Instant eventDate = Instant.ofEpochMilli(startTimestamp / 1000000);
        return !(eventDate.isBefore(startDate) || eventDate.isAfter(endDate));
    }
}
