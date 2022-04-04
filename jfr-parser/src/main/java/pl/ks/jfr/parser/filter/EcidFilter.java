package pl.ks.jfr.parser.filter;

import lombok.Builder;
import lombok.Value;
import org.openjdk.jmc.common.IMCThread;
import org.openjdk.jmc.common.item.IItem;
import org.openjdk.jmc.common.item.IMemberAccessor;
import org.openjdk.jmc.common.unit.IQuantity;

@Value
@Builder
public class EcidFilter implements PreStackFilter {
    String ecid;

    @Override
    public boolean shouldInclude(IMemberAccessor<IQuantity, IItem> startTimeAccessor,
                                 IMemberAccessor<IMCThread, IItem> threadAccessor,
                                 IMemberAccessor<String, IItem> ecidAccessor,
                                 IItem event) {
        if (ecidAccessor == null) {
            return true;
        }

        String ecid = ecidAccessor.getMember(event);
        return ecid.equalsIgnoreCase(this.ecid);
    }
}
