package pl.ks.jfr.parser;

import java.time.Instant;
import lombok.Getter;

@Getter
public class JfrEcidInfo {
    private String ecid;
    private Instant minDate = Instant.MAX;
    private Instant maxDate = Instant.MIN;
    private long wallSamples;
    private long cpuSamples;

    public JfrEcidInfo(String ecid) {
        this.ecid = ecid;
    }

    public long timeDiff() {
        return maxDate.toEpochMilli() - minDate.toEpochMilli();
    }

    synchronized void newWallSample(Instant when, boolean runnable) {
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
