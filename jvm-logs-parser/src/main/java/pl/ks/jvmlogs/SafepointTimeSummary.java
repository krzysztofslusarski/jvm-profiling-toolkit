package pl.ks.jvmlogs;

import com.microsoft.gctoolkit.aggregator.Collates;
import com.microsoft.gctoolkit.time.DateTimeStamp;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Collates(SafepointTimeAggregator.class)
public class SafepointTimeSummary implements SafepointTimeAggregation {
    private final Map<String, XYDataSet> ttsAggregation = new ConcurrentHashMap<>();
    private final Map<String, XYDataSet> vmOpsAggregation = new ConcurrentHashMap<>();
    private final Map<String, XYDataSet> totalAggregation = new ConcurrentHashMap<>();

    @Override
    public void addDataPoint(String vmOperation, DateTimeStamp timeStamp, SafepointTime safepointTime) {
        ttsAggregation.computeIfAbsent(vmOperation, key -> new XYDataSet()).add(timeStamp.getTimeStamp(), safepointTime.getTts());
        vmOpsAggregation.computeIfAbsent(vmOperation, key -> new XYDataSet()).add(timeStamp.getTimeStamp(), safepointTime.getPauseTime());
        totalAggregation.computeIfAbsent(vmOperation, key -> new XYDataSet()).add(timeStamp.getTimeStamp(), safepointTime.getTotalTime());
    }

    @Override
    public boolean hasWarning() {
        return false;
    }

    @Override
    public boolean isEmpty() {
        return ttsAggregation.isEmpty();
    }

    @Override
    public String toString() {
        return "Collected " + ttsAggregation.size() + " different collection types";
    }

}
