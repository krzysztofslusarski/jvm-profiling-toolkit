package pl.ks.jvmlogs;

import com.microsoft.gctoolkit.aggregator.Aggregator;
import com.microsoft.gctoolkit.event.jvm.ApplicationStoppedTime;
import com.microsoft.gctoolkit.event.jvm.JVMEvent;

public class SafepointTimeAggregator extends Aggregator<SafepointTimeAggregation> {
    public SafepointTimeAggregator(SafepointTimeAggregation aggregation) {
        super(aggregation);
    }

    @Override
    public <E extends JVMEvent> void consume(E event) {
        if (event instanceof ApplicationStoppedTime) {
            super.consume(event);
        }
    }

    private void process(ApplicationStoppedTime event) {
        aggregation()
                .addDataPoint(event.getSafePointReason().name(), event.getDateTimeStamp(), SafepointTime.builder()
                        .pauseTime(event.getTimeToStopThreads())
                        .tts(event.getDuration())
                        .build()
                );
    }
}
