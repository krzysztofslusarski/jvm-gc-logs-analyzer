/*
 * Copyright 2020 Krzysztof Slusarski
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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
