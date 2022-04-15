package pl.ks.jfr.parser.filter;

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
