package pl.ks.jfr.parser.filter;

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
