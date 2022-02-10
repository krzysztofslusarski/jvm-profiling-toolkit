package pl.ks.jfr.parser.filter;

import org.openjdk.jmc.common.IMCThread;
import org.openjdk.jmc.common.item.IItem;
import org.openjdk.jmc.common.item.IMemberAccessor;
import org.openjdk.jmc.common.unit.IQuantity;

public interface PreStackFilter {
    boolean shouldInclude(IMemberAccessor<IQuantity, IItem> startTimeAccessor,
                          IMemberAccessor<IMCThread, IItem> threadAccessor,
                          IItem event);
}
