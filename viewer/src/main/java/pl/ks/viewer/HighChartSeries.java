package pl.ks.viewer;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class HighChartSeries {
    String name;
    Object[] data;
}
