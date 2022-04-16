package pl.ks.jfr.parser.tuning;

import lombok.Builder;
import lombok.Value;
import org.openjdk.jmc.common.item.IItem;
import org.openjdk.jmc.common.item.IMemberAccessor;
import org.openjdk.jmc.common.unit.IQuantity;
import pl.ks.jfr.parser.JfrAccessors;

@Value
@Builder
public class StartEndTimestampFilter implements PreStackFilter{
    long startTs;
    long endTs;

    @Override
    public boolean shouldInclude(JfrAccessors accessors, IItem event) {
        return shouldInclude(accessors.getStartTimeAccessor(), event);
    }

    private boolean shouldInclude(IMemberAccessor<IQuantity, IItem> startTimeAccessor, IItem event) {
        long eventTs = startTimeAccessor.getMember(event).longValue() / 1000000 / 1000;
        return !(eventTs < startTs || eventTs > endTs);
    }
}
