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

import java.util.Date;
import java.util.List;
import java.util.Map;

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

    static boolean isExecutionSampleEvent(EventArray event) {
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

    static String fetchFlatStackTrace(IItem event, JfrAccessors accessors, JfrCollapsedParserContext context) {
        String threadName = accessors.getThreadAccessor().getMember(event).getThreadName();
        List<? extends IMCFrame> frames = accessors.getStackTraceAccessor().getMember(event).getFrames();

        StringBuilder builder = new StringBuilder();

        if (context.isIncludeEcid() && accessors.getEcidAccessor() != null) {
            builder.append(accessors.getEcidAccessor().getMember(event).longValue()).append(";");
        }

        if (context.isIncludeAnyTimestampAndDate()) {
            IQuantity startTime = accessors.getStartTimeAccessor().getMember(event);
            if (context.isIncludeTimestamp100MSAndDate()) {
                long time = startTime.longValue() / 1000000 / 100;
                builder.append(JfrCollapsedParserImpl.TIME_STAMP_FORMAT.get().format(time)).append("_");
                builder.append(JfrCollapsedParserImpl.OUTPUT_FORMAT.get().format(new Date(time * 100))).append("_[k];");
            }
            if (context.isIncludeTimestamp1SAndDate()) {
                long time = startTime.longValue() / 1000000 / 1000;
                builder.append(JfrCollapsedParserImpl.TIME_STAMP_FORMAT.get().format(time)).append("_");
                builder.append(JfrCollapsedParserImpl.OUTPUT_FORMAT.get().format(new Date(time * 1000))).append("_[k];");
            }
            if (context.isIncludeTimestamp10SAndDate()) {
                long time = startTime.longValue() / 1000000 / 10000;
                builder.append(JfrCollapsedParserImpl.TIME_STAMP_FORMAT.get().format(time)).append("_");
                builder.append(JfrCollapsedParserImpl.OUTPUT_FORMAT.get().format(new Date(time * 10000))).append("_[k];");
            }
        }

        if (context.isIncludeFileName()) {
            String filename = context.getFile().getFileName().toString();
            builder.append(filename).append("_[i];");
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
                String packageName = method.getType().getPackage().getName() == null ? "" : replaceCharacter(method.getType().getPackage().getName(), '/', '.');
                if (packageName.length() > 0) {
                    builder.append(packageName);
                    builder.append("/");
                }
            } catch (RuntimeException e) {
                e.printStackTrace();
                throw e;
            }
            if (!method.getFormalDescriptor().equals("()L;")) {
                String className = replaceCharacter(method.getType().getTypeName(), '/', '.');
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

    static IMemberAccessor<IQuantity, IItem> findEcidAccessor(EventArray eventArray) {
        for (Map.Entry<IAccessorKey<?>, ? extends IDescribable> accessorKey : eventArray.getType().getAccessorKeys().entrySet()) {
            if (accessorKey.getKey().getIdentifier().equals("contextId")) {
                return (IMemberAccessor<IQuantity, IItem>) eventArray.getType().getAccessor(accessorKey.getKey());
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

    static IMemberAccessor<IQuantity, IItem> findLockDurationAccessor(EventArray eventArray) {
        for (Map.Entry<IAccessorKey<?>, ? extends IDescribable> accessorKey : eventArray.getType().getAccessorKeys().entrySet()) {
            if (accessorKey.getKey().getIdentifier().equals("duration")) {
                return (IMemberAccessor<IQuantity, IItem>) eventArray.getType().getAccessor(accessorKey.getKey());
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

    static String replaceCharacter(String str, char toReplace, char replacedWith) {
        char[] chars = str.toCharArray();
        for (int i = 0; i < chars.length; i++) {
            if (chars[i] == toReplace) {
                chars[i] = replacedWith;
            }
        }
        return new String(chars);
    }
}
