package pl.ks.profiling.safepoint.analyzer.commons.shared;

import java.math.BigDecimal;
import java.util.stream.DoubleStream;
import org.apache.commons.math3.stat.StatUtils;

public class OneFiledAllStatsUtil {
    public static OneFiledAllStats create(double[] values) {
        OneFiledAllStats stats = new OneFiledAllStats();
        if (values.length == 0) {
            return stats;
        }
        stats.setPercentile50(new BigDecimal(StatUtils.percentile(values, 50)));
        stats.setPercentile75(new BigDecimal(StatUtils.percentile(values, 75)));
        stats.setPercentile90(new BigDecimal(StatUtils.percentile(values, 90)));
        stats.setPercentile95(new BigDecimal(StatUtils.percentile(values, 95)));
        stats.setPercentile99(new BigDecimal(StatUtils.percentile(values, 99)));
        stats.setPercentile99and9(new BigDecimal(StatUtils.percentile(values, 99.9)));
//        stats.setPercentile99and99(new BigDecimal(StatUtils.percentile(values, 99.99)));
//        stats.setPercentile99and999(new BigDecimal(StatUtils.percentile(values, 99.999)));
        stats.setPercentile100(new BigDecimal(StatUtils.percentile(values, 100)));
        stats.setAverage(new BigDecimal(DoubleStream.of(values).average().orElse(0)));
        stats.setTotal(new BigDecimal(DoubleStream.of(values).sum()));
        stats.setCount(new BigDecimal(values.length));
        return stats;
    }
}
