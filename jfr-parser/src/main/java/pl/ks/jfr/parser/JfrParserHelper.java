/*
 * Copyright 2020 Krzysztof Slusarski
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

import java.util.Date;
import java.util.List;
import java.util.Map;
import org.openjdk.jmc.common.IDescribable;
import org.openjdk.jmc.common.IMCFrame;
import org.openjdk.jmc.common.IMCMethod;
import org.openjdk.jmc.common.IMCType;
import org.openjdk.jmc.common.item.IAccessorKey;
import org.openjdk.jmc.common.item.IItem;
import org.openjdk.jmc.common.item.IMemberAccessor;
import org.openjdk.jmc.common.unit.IQuantity;
import org.openjdk.jmc.common.unit.ITypedQuantity;
import org.openjdk.jmc.common.unit.StructContentType;
import org.openjdk.jmc.flightrecorder.internal.EventArray;

class JfrParserHelper {
    static boolean isAsyncAllocNewTLABEvent(EventArray event) {
        if (event.getType() instanceof StructContentType) {
            StructContentType structContentType = (StructContentType) event.getType();
            return structContentType.getIdentifier().equals("jdk.ObjectAllocationInNewTLAB");
        }
        return false;
    }

    static boolean isLockEvent(EventArray event) {
        if (event.getType() instanceof StructContentType) {
            StructContentType structContentType = (StructContentType) event.getType();
            return structContentType.getIdentifier().equals("jdk.JavaMonitorEnter");
        }
        return false;
    }

    static boolean isAsyncAllocOutsideTLABEvent(EventArray event) {
        if (event.getType() instanceof StructContentType) {
            StructContentType structContentType = (StructContentType) event.getType();
            return structContentType.getIdentifier().equals("jdk.ObjectAllocationOutsideTLAB");
        }
        return false;
    }

    static boolean isAsyncWallEvent(EventArray event) {
        if (event.getType() instanceof StructContentType) {
            StructContentType structContentType = (StructContentType) event.getType();
            return structContentType.getIdentifier().equals("jdk.ExecutionSample");
        }
        return false;
    }

    static boolean isCpuLoadEvent(EventArray event) {
        if (event.getType() instanceof StructContentType) {
            StructContentType structContentType = (StructContentType) event.getType();
            return structContentType.getIdentifier().equals("jdk.CPULoad");
        }
        return false;
    }

    static boolean isOsInfoEvent(EventArray event) {
        if (event.getType() instanceof StructContentType) {
            StructContentType structContentType = (StructContentType) event.getType();
            return structContentType.getIdentifier().equals("jdk.OSInformation");
        }
        return false;
    }

    static boolean isCpuInfoEvent(EventArray event) {
        if (event.getType() instanceof StructContentType) {
            StructContentType structContentType = (StructContentType) event.getType();
            return structContentType.getIdentifier().equals("jdk.CPUInformation");
        }
        return false;
    }

    static boolean isJvmInfoEvent(EventArray event) {
        if (event.getType() instanceof StructContentType) {
            StructContentType structContentType = (StructContentType) event.getType();
            return structContentType.getIdentifier().equals("jdk.JVMInformation");
        }
        return false;
    }

    static boolean isInitialSystemProperty(EventArray event) {
        if (event.getType() instanceof StructContentType) {
            StructContentType structContentType = (StructContentType) event.getType();
            return structContentType.getIdentifier().equals("jdk.InitialSystemProperty");
        }
        return false;
    }

    static String fetchFlatStackTrace(IItem event, JfrAccessors accessors, JfrParserContext context) {
        String threadName = accessors.getThreadAccessor().getMember(event).getThreadName();
        List<? extends IMCFrame> frames = accessors.getStackTraceAccessor().getMember(event).getFrames();

        StringBuilder builder = new StringBuilder();
        if (context.isIncludeTimestampAndDate()) {
            IQuantity startTime = accessors.getStartTimeAccessor().getMember(event);
            long time = startTime.longValue() / 1000000 / context.getTimestampDivider();
            builder.append(JfrParserImpl.TIME_STAMP_FORMAT.get().format(time)).append("_");
            builder.append(JfrParserImpl.OUTPUT_FORMAT.get().format(new Date(time * context.getTimestampDivider()))).append("_[k];");
        }

        if (context.isIncludeFileName()) {
            String fileName = context.getFile().getFileName().toString();
            builder.append(fileName).append("_[i];");
        }

        if (context.isIncludeThreadName()) {
            builder.append(threadName).append(";");
        }

        for (int i = frames.size() - 1; i >= 0; i--) {
            IMCFrame frame = frames.get(i);
            IMCMethod method = frame.getMethod();

            if (i != frames.size() - 1) {
                builder.append(";");
            }

            try {
                String packageName = method.getType().getPackage().getName() == null ? "" : method.getType().getPackage().getName().replace(".", "/");
                if (packageName.length() > 0) {
                    builder.append(packageName);
                    builder.append("/");
                }
            } catch (RuntimeException e) {
                e.printStackTrace();
                throw e;
            }
            if (!method.getFormalDescriptor().equals("()L;")) {
                String className = method.getType().getTypeName().replace(".", "/");
                if (className.length() > 0) {
                    builder.append(className);
                    builder.append(".");
                }
            }
            builder.append(method.getMethodName());
            if (method.getFormalDescriptor().equals("(Lk;)L;")) {
                builder.append("_[k]");
            }
        }
        return builder.toString();
    }

    static IMemberAccessor<String, IItem> findStateAccessor(EventArray eventArray) {
        for (Map.Entry<IAccessorKey<?>, ? extends IDescribable> accessorKey : eventArray.getType().getAccessorKeys().entrySet()) {
            if (accessorKey.getKey().getIdentifier().equals("state")) {
                return (IMemberAccessor<String, IItem>) eventArray.getType().getAccessor(accessorKey.getKey());
            }
        }
        return null;
    }

    static IMemberAccessor<String, IItem> findEcidAccessor(EventArray eventArray) {
        for (Map.Entry<IAccessorKey<?>, ? extends IDescribable> accessorKey : eventArray.getType().getAccessorKeys().entrySet()) {
            if (accessorKey.getKey().getIdentifier().equals("ecid")) {
                return (IMemberAccessor<String, IItem>) eventArray.getType().getAccessor(accessorKey.getKey());
            }
        }
        return null;
    }

    static IMemberAccessor<IMCType, IItem> findMonitorClassAccessor(EventArray eventArray) {
        for (Map.Entry<IAccessorKey<?>, ? extends IDescribable> accessorKey : eventArray.getType().getAccessorKeys().entrySet()) {
            if (accessorKey.getKey().getIdentifier().equals("monitorClass")) {
                return (IMemberAccessor<IMCType, IItem>) eventArray.getType().getAccessor(accessorKey.getKey());
            }
        }
        return null;
    }

    static IMemberAccessor<ITypedQuantity, IItem> findCpuJvmUserAccessor(EventArray eventArray) {
        for (Map.Entry<IAccessorKey<?>, ? extends IDescribable> accessorKey : eventArray.getType().getAccessorKeys().entrySet()) {
            if (accessorKey.getKey().getIdentifier().equals("jvmUser")) {
                return (IMemberAccessor<ITypedQuantity, IItem>) eventArray.getType().getAccessor(accessorKey.getKey());
            }
        }
        return null;
    }

    static IMemberAccessor<ITypedQuantity, IItem> findCpuJvmSystemAccessor(EventArray eventArray) {
        for (Map.Entry<IAccessorKey<?>, ? extends IDescribable> accessorKey : eventArray.getType().getAccessorKeys().entrySet()) {
            if (accessorKey.getKey().getIdentifier().equals("jvmSystem")) {
                return (IMemberAccessor<ITypedQuantity, IItem>) eventArray.getType().getAccessor(accessorKey.getKey());
            }
        }
        return null;
    }

    static IMemberAccessor<ITypedQuantity, IItem> findMachineTotalAccessor(EventArray eventArray) {
        for (Map.Entry<IAccessorKey<?>, ? extends IDescribable> accessorKey : eventArray.getType().getAccessorKeys().entrySet()) {
            if (accessorKey.getKey().getIdentifier().equals("machineTotal")) {
                return (IMemberAccessor<ITypedQuantity, IItem>) eventArray.getType().getAccessor(accessorKey.getKey());
            }
        }
        return null;
    }

    static IMemberAccessor<String, IItem> findKeyAccessor(EventArray eventArray) {
        for (Map.Entry<IAccessorKey<?>, ? extends IDescribable> accessorKey : eventArray.getType().getAccessorKeys().entrySet()) {
            if (accessorKey.getKey().getIdentifier().equals("key")) {
                return (IMemberAccessor<String, IItem>) eventArray.getType().getAccessor(accessorKey.getKey());
            }
        }
        return null;
    }

    static IMemberAccessor<String, IItem> findValueAccessor(EventArray eventArray) {
        for (Map.Entry<IAccessorKey<?>, ? extends IDescribable> accessorKey : eventArray.getType().getAccessorKeys().entrySet()) {
            if (accessorKey.getKey().getIdentifier().equals("value")) {
                return (IMemberAccessor<String, IItem>) eventArray.getType().getAccessor(accessorKey.getKey());
            }
        }
        return null;
    }

    static IMemberAccessor<IQuantity, IItem> findAllocSizeAccessor(EventArray eventArray) {
        for (Map.Entry<IAccessorKey<?>, ? extends IDescribable> accessorKey : eventArray.getType().getAccessorKeys().entrySet()) {
            if (accessorKey.getKey().getIdentifier().equals("allocationSize")) {
                return (IMemberAccessor<IQuantity, IItem>) eventArray.getType().getAccessor(accessorKey.getKey());
            }
        }
        return null;
    }

    static IMemberAccessor<IMCType, IItem> findObjectClassAccessor(EventArray eventArray) {
        for (Map.Entry<IAccessorKey<?>, ? extends IDescribable> accessorKey : eventArray.getType().getAccessorKeys().entrySet()) {
            if (accessorKey.getKey().getIdentifier().equals("objectClass")) {
                return (IMemberAccessor<IMCType, IItem>) eventArray.getType().getAccessor(accessorKey.getKey());
            }
        }
        return null;
    }

    static boolean isConsumingCpu(String state) {
        return "STATE_RUNNABLE".equals(state);
    }
}
