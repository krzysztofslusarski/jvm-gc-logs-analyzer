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
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class OneFiledAllStats {
    private BigDecimal percentile50;
    private BigDecimal percentile75;
    private BigDecimal percentile90;
    private BigDecimal percentile95;
    private BigDecimal percentile99;
    private BigDecimal percentile99and9;
    private BigDecimal percentile100;
    private BigDecimal average;
    private BigDecimal total;
    private BigDecimal count;
}
