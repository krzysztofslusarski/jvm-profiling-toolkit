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
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import static pl.ks.jfr.parser.JfrParsedCommonStackTraceEvent.SPAN_INFO_COMPARATOR;

/**
 * Spans grouped per thread and sorted by start time, so that finding the spans an event belongs to is a hash lookup
 * plus a binary search - the recordings have far more events than spans, and every event has to be checked.
 */
class JfrSpanIndex {
    private static final long NANOS_IN_MS = 1_000_000;

    private final Map<String, ThreadSpans> spansPerThread;

    private JfrSpanIndex(Map<String, ThreadSpans> spansPerThread) {
        this.spansPerThread = spansPerThread;
    }

    static JfrSpanIndex of(List<JfrSpanInfo> spans) {
        Map<String, List<JfrSpanInfo>> perThread = new HashMap<>();
        for (JfrSpanInfo span : spans) {
            perThread.computeIfAbsent(span.getThreadName(), threadName -> new ArrayList<>()).add(span);
        }

        Map<String, ThreadSpans> spansPerThread = new HashMap<>();
        perThread.forEach((threadName, threadSpans) -> spansPerThread.put(threadName, new ThreadSpans(threadSpans)));
        return new JfrSpanIndex(spansPerThread);
    }

    /**
     * Spans that were open on the given thread at the given moment, or <code>null</code> when there was none - so that
     * the caller can leave the event untouched instead of copying it.
     */
    Set<JfrSpanInfo> spansAt(String threadName, Instant eventTime) {
        ThreadSpans threadSpans = spansPerThread.get(threadName);
        return threadSpans == null ? null : threadSpans.spansAt(eventTime.toEpochMilli());
    }

    private static class ThreadSpans {
        private final JfrSpanInfo[] spans;
        private final long[] starts;
        private final long[] ends;
        private final long longestSpan;

        private ThreadSpans(List<JfrSpanInfo> threadSpans) {
            spans = threadSpans.stream()
                    .sorted(Comparator.comparing(JfrSpanInfo::getEventTime))
                    .toArray(JfrSpanInfo[]::new);

            starts = new long[spans.length];
            ends = new long[spans.length];
            long longest = 0;
            for (int i = 0; i < spans.length; i++) {
                starts[i] = spans[i].getEventTime().toEpochMilli();
                ends[i] = starts[i] + spans[i].getDuration() / NANOS_IN_MS;
                longest = Math.max(longest, ends[i] - starts[i]);
            }
            longestSpan = longest;
        }

        private Set<JfrSpanInfo> spansAt(long timestamp) {
            Set<JfrSpanInfo> found = null;
            for (int i = lastStartedNotAfter(timestamp); i >= 0; i--) {
                if (starts[i] + longestSpan < timestamp) {
                    // spans are sorted by start time, no earlier one is long enough to reach the event
                    break;
                }
                if (ends[i] >= timestamp) {
                    if (found == null) {
                        found = new TreeSet<>(SPAN_INFO_COMPARATOR);
                    }
                    found.add(spans[i]);
                }
            }
            return found;
        }

        private int lastStartedNotAfter(long timestamp) {
            int low = 0;
            int high = starts.length - 1;
            int found = -1;
            while (low <= high) {
                int mid = (low + high) >>> 1;
                if (starts[mid] > timestamp) {
                    high = mid - 1;
                } else {
                    found = mid;
                    low = mid + 1;
                }
            }
            return found;
        }
    }
}
