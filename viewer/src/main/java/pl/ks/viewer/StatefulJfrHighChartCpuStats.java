package pl.ks.viewer;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import lombok.Builder;
import lombok.Value;
import pl.ks.jfr.parser.JfrParsedCpuUsageEvent;

@Value
@Builder
public class StatefulJfrHighChartCpuStats {
    Map<String, List<JfrParsedCpuUsageEvent>> cpuUsageEventsPerFile;
    List<String> filenames;

    public List<HighChartSeries> generateMachineTotalHighChart() {
        return generateHighChart(JfrParsedCpuUsageEvent::getMachineTotal);
    }

    public List<HighChartSeries> generateJvmUserHighChart() {
        return generateHighChart(JfrParsedCpuUsageEvent::getJvmUser);
    }

    public List<HighChartSeries> generateJvmSystemHighChart() {
        return generateHighChart(JfrParsedCpuUsageEvent::getJvmSystem);
    }

    public List<HighChartSeries> generateJvmTotalHighChart() {
        return generateHighChart(JfrParsedCpuUsageEvent::getJvmTotal);
    }

    public List<HighChartSeries> generateNotJvmTotalHighChart() {
        return generateHighChart(JfrParsedCpuUsageEvent::getNotJvmTotal);
    }

    private List<HighChartSeries> generateHighChart(Function<JfrParsedCpuUsageEvent, BigDecimal> valueFunction) {
        int numberOfSeries = filenames.size();
        List<HighChartSeries> ret = new ArrayList<>(numberOfSeries);
        for (String filename : filenames) {
            List<JfrParsedCpuUsageEvent> eventsForFile = cpuUsageEventsPerFile.get(filename);
            Object[][] series = new Object[eventsForFile.size()][];
            int j = 0;
            for (JfrParsedCpuUsageEvent event : eventsForFile) {
                Object[] seriesData = new Object[2];
                seriesData[0] = event.getEventTime().toEpochMilli();
                seriesData[1] = valueFunction.apply(event);
                series[j++] = seriesData;
            }
            ret.add(new HighChartSeries(filename, series));
        }

        return ret;
    }
}
