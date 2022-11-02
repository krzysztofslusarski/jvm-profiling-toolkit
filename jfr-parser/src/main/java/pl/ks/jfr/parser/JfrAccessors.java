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
    IMemberAccessor<IQuantity, IItem> lockDurationAccessor;
    IMemberAccessor<String, IItem> stateAccessor;
    IMemberAccessor<IQuantity, IItem> ecidAccessor;
    IMemberAccessor<ITypedQuantity, IItem> jvmUserAccessor;
    IMemberAccessor<ITypedQuantity, IItem> jvmSystemAccessor;
    IMemberAccessor<ITypedQuantity, IItem> machineTotalAccessor;
}
