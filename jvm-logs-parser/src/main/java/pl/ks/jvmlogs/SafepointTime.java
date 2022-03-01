package pl.ks.jvmlogs;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class SafepointTime {
    double tts;
    double pauseTime;

    public double getTotalTime() {
        return tts + pauseTime;
    }
}
