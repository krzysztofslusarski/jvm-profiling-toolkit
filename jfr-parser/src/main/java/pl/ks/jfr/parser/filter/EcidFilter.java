package pl.ks.jfr.parser.filter;

import lombok.Builder;
import lombok.Value;
import org.openjdk.jmc.common.item.IItem;
import org.openjdk.jmc.common.item.IMemberAccessor;
import pl.ks.jfr.parser.JfrAccessors;

@Value
@Builder
public class EcidFilter implements PreStackFilter {
    String ecid;

    @Override
    public boolean shouldInclude(JfrAccessors accessors, IItem event) {
        return shouldInclude(accessors.getEcidAccessor(), event);
    }

    private boolean shouldInclude(IMemberAccessor<String, IItem> ecidAccessor, IItem event) {
        if (ecidAccessor == null) {
            return false;
        }

        String ecid = ecidAccessor.getMember(event);
        return this.ecid.equalsIgnoreCase(ecid);
    }
}
