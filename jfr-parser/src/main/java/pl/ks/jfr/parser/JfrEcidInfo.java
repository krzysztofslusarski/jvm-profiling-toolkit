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

import java.time.Instant;
import lombok.Getter;

@Getter
public class JfrEcidInfo {
    private final long ecid;
    private Instant minDate = Instant.MAX;
    private Instant maxDate = Instant.MIN;
    private long wallSamples;
    private long cpuSamples;

    public JfrEcidInfo(long ecid) {
        this.ecid = ecid;
    }

    public synchronized long timeDiff() {
        return maxDate.toEpochMilli() - minDate.toEpochMilli();
    }

    public synchronized void newExecutionSample(Instant when, boolean runnable) {
        if (minDate.isAfter(when)) {
            minDate = when;
        }
        if (maxDate.isBefore(when)) {
            maxDate = when;
        }
        wallSamples++;
        if (runnable) {
            cpuSamples++;
        }
    }
}
