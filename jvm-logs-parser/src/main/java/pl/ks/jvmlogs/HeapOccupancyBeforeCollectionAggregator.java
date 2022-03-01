package pl.ks.jvmlogs;

import com.microsoft.gctoolkit.aggregator.Aggregates;
import com.microsoft.gctoolkit.aggregator.Aggregator;
import com.microsoft.gctoolkit.aggregator.EventSource;
import com.microsoft.gctoolkit.event.g1gc.G1GCPauseEvent;
import com.microsoft.gctoolkit.event.generational.GenerationalGCPauseEvent;
import com.microsoft.gctoolkit.event.zgc.ZGCCycle;

@Aggregates({EventSource.G1GC, EventSource.GENERATIONAL, EventSource.ZGC})
public class HeapOccupancyBeforeCollectionAggregator extends Aggregator<HeapOccupancyBeforeCollectionAggregation> {
    public HeapOccupancyBeforeCollectionAggregator(HeapOccupancyBeforeCollectionAggregation aggregation) {
        super(aggregation);
        register(GenerationalGCPauseEvent.class, this::extractHeapOccupancy);
        register(G1GCPauseEvent.class, this::extractHeapOccupancy);
        register(ZGCCycle.class, this::extractHeapOccupancy);
    }

    private void extractHeapOccupancy(GenerationalGCPauseEvent event) {
        aggregation()
                .addDataPoint(
                        event.getGarbageCollectionType(),
                        event.getDateTimeStamp(),
                        event.getHeap().getOccupancyBeforeCollection()
                );
    }

    private void extractHeapOccupancy(G1GCPauseEvent event) {
        aggregation()
                .addDataPoint(
                        event.getGarbageCollectionType(),
                        event.getDateTimeStamp(),
                        event.getHeap().getOccupancyBeforeCollection()
                );
    }

    private void extractHeapOccupancy(ZGCCycle event) {
        aggregation()
                .addDataPoint(
                        event.getGarbageCollectionType(),
                        event.getDateTimeStamp(),
                        event.getLive().getReclaimStart()
                );
    }
}