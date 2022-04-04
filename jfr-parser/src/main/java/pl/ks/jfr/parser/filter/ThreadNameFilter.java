package pl.ks.jfr.parser.filter;

import lombok.Builder;
import lombok.Value;
import org.openjdk.jmc.common.IMCThread;
import org.openjdk.jmc.common.item.IItem;
import org.openjdk.jmc.common.item.IMemberAccessor;
import org.openjdk.jmc.common.unit.IQuantity;

@Value
@Builder
public class ThreadNameFilter implements PreStackFilter {
    String threadName;

    @Override
    public boolean shouldInclude(IMemberAccessor<IQuantity, IItem> startTimeAccessor,
                                 IMemberAccessor<IMCThread, IItem> threadAccessor,
                                 IMemberAccessor<String, IItem> ecidAccessor,
                                 IItem event) {
        if (threadAccessor == null) {
            return true;
        }

        String threadName = threadAccessor.getMember(event).getThreadName().toLowerCase();
        return threadName.equalsIgnoreCase(this.threadName);
    }
}
