package pl.ks.jvmlogs;

import com.microsoft.gctoolkit.GCToolKit;
import com.microsoft.gctoolkit.io.SingleGCLogFile;

import java.nio.file.Paths;

public class Test {
    public static void main(String[] args) {
        var logFile = new SingleGCLogFile(Paths.get("/home/pasq/verbosegc.log"));
        var gcToolKit = new GCToolKit();
        gcToolKit.registerAggregation(HeapOccupancyAfterCollectionSummary.class);
        gcToolKit.registerAggregation(HeapOccupancyBeforeCollectionSummary.class);
        gcToolKit.registerAggregation(SafepointTimeSummary.class);



        var jvm = gcToolKit.analyze(logFile);
        var after = jvm.getAggregation(HeapOccupancyAfterCollectionSummary.class);
        var before = jvm.getAggregation(HeapOccupancyBeforeCollectionSummary.class);
        var safepoint = jvm.getAggregation(SafepointTimeSummary.class);

        System.out.println("Done");
    }
}
