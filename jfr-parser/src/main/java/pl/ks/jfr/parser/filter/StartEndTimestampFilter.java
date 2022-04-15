package pl.ks.jfr.parser.filter;

import lombok.Builder;
import lombok.Value;
import org.openjdk.jmc.common.IMCThread;
import org.openjdk.jmc.common.item.IItem;
import org.openjdk.jmc.common.item.IMemberAccessor;
import org.openjdk.jmc.common.unit.IQuantity;

@Value
@Builder
public class StartEndTimestampFilter implements PreStackFilter{
    long startTs;
    long endTs;

    @Override
    public boolean shouldInclude(IMemberAccessor<IQuantity, IItem> startTimeAccessor,
                                 IMemberAccessor<IMCThread, IItem> threadAccessor,
                                 IMemberAccessor<String, IItem> ecidAccessor,
                                 IItem event) {
        long eventTs = startTimeAccessor.getMember(event).longValue() / 1000000 / 1000;
        return !(eventTs < startTs || eventTs > endTs);
    }
}
