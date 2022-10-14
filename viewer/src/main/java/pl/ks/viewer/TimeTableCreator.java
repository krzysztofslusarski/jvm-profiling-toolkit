package pl.ks.viewer;

import static java.util.Comparator.comparingLong;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.function.ToLongFunction;
import pl.ks.viewer.pages.PageCreatorHelper;

public class TimeTableCreator {
    private static final BigDecimal PERCENT_MULTIPLICAND = new BigDecimal(100);

    public static TimeTable create(SelfAndTotalTimeStats stats, TimeTable.Type type, long limit, UUID fileId) {
        var decimalFormat = new DecimalFormat("#,##0.00", DecimalFormatSymbols.getInstance(Locale.US));
        var totalCount = new BigDecimal(stats.getTotalCounter().get());
        var extractor = getTimeExtractor(type);
        var comparator = comparingLong(getTimeExtractor(type)).reversed();
        var content = stats.getMethodStats().entrySet().stream()
                .sorted(comparator)
                .limit(limit)
                .map(entry -> TimeTable.Row.builder()
                        .methodName(entry.getValue().getName())
                        .samples(extractor.applyAsLong(entry))
                        .percent(getPercent(extractor.applyAsLong(entry), totalCount, decimalFormat))
                        .build()
                )
                .toList();

        return TimeTable.builder()
                .rows(content)
                .fileId(fileId)
                .build();
    }

    private static ToLongFunction<Map.Entry<String, SelfAndTotalTimeStats.SelfAndTotalTimeMethodStats>> getTimeExtractor(TimeTable.Type type) {
        switch (type) {
            case TOTAL_TIME -> {
                return o -> o.getValue().getTotalTimeSamples().get();
            }
            case SELF_TIME -> {
                return o -> o.getValue().getSelfTimeSamples().get();
            }
            default -> {
                throw new IllegalStateException();
            }
        }
    }

    private static String getPercent(long samples, BigDecimal totalCount, DecimalFormat decimalFormat) {
        var percent = new BigDecimal(samples).multiply(PERCENT_MULTIPLICAND).divide(totalCount, 2, RoundingMode.HALF_EVEN);
        return PageCreatorHelper.numToString(percent, decimalFormat);
    }
}
