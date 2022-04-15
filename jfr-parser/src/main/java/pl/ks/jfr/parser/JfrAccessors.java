package pl.ks.jfr.parser;

import lombok.Builder;
import lombok.Value;
import org.openjdk.jmc.common.IMCStackTrace;
import org.openjdk.jmc.common.IMCThread;
import org.openjdk.jmc.common.IMCType;
import org.openjdk.jmc.common.item.IItem;
import org.openjdk.jmc.common.item.IMemberAccessor;
import org.openjdk.jmc.common.unit.IQuantity;
import org.openjdk.jmc.common.unit.ITypedQuantity;

@Value
@Builder
public class JfrAccessors {
    IMemberAccessor<IMCStackTrace, IItem> stackTraceAccessor;
    IMemberAccessor<IMCThread, IItem> threadAccessor;
    IMemberAccessor<IQuantity, IItem> startTimeAccessor;
    IMemberAccessor<IQuantity, IItem> allocationSizeAccessor;
    IMemberAccessor<IMCType, IItem> objectClassAccessor;
    IMemberAccessor<IMCType, IItem> monitorClassAccessor;
    IMemberAccessor<String, IItem> stateAccessor;
    IMemberAccessor<String, IItem> ecidAccessor;
    IMemberAccessor<ITypedQuantity, IItem> jvmUserAccessor;
    IMemberAccessor<ITypedQuantity, IItem> jvmSystemAccessor;
    IMemberAccessor<ITypedQuantity, IItem> machineTotalAccessor;
}
