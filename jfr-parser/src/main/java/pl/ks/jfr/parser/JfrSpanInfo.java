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

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import lombok.Builder;
import lombok.Value;

/**
 * Single <code>profiler.Span</code> event emitted by async-profiler ({@code one.profiler.Span.start()/end(id, tag)}).
 */
@Value
@Builder
public class JfrSpanInfo implements JfrParsedEventWithTime, JfrParsedEventWithThread {
    private static final BigDecimal NANOS_IN_MS = BigDecimal.valueOf(1_000_000);

    String tag;
    String threadName;
    String filename;
    Instant eventTime;
    long duration;

    public BigDecimal getDurationInMs() {
        return BigDecimal.valueOf(duration).divide(NANOS_IN_MS, 3, RoundingMode.HALF_EVEN);
    }
}
