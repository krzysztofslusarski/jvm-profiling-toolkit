package pl.ks.jvmlogs;

import com.microsoft.gctoolkit.aggregator.Aggregation;
import com.microsoft.gctoolkit.time.DateTimeStamp;

interface SafepointTimeAggregation extends Aggregation {
    void addDataPoint(String vmOperation, DateTimeStamp timeStamp, SafepointTime safepointTime);
}
