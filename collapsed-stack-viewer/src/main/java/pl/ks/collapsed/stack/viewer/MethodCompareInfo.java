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
package pl.ks.collapsed.stack.viewer;

import java.math.BigDecimal;
import java.math.RoundingMode;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class MethodCompareInfo {
    private static final BigDecimal PERCENT_MULTIPLICAND = new BigDecimal(100);

    private String name;
    private long selfSamples1;
    private long totalSamples1;
    private long selfSamples2;
    private long totalSamples2;
    private BigDecimal selfPercent1;
    private BigDecimal selfPercent2;
    private BigDecimal totalPercent1;
    private BigDecimal totalPercent2;

    void addSelfSamples1(long selfSamples) {
        this.selfSamples1 += selfSamples;
    }

    void addTotalSamples1(long totalSamples) {
        this.totalSamples1 += totalSamples;
    }

    void addSelfSamples2(long selfSamples) {
        this.selfSamples2 += selfSamples;
    }

    void addTotalSamples2(long totalSamples) {
        this.totalSamples2 += totalSamples;
    }

    public void calculatePercents(BigDecimal total1, BigDecimal total2) {
        selfPercent1 = new BigDecimal(selfSamples1).multiply(PERCENT_MULTIPLICAND).divide(total1, 2, RoundingMode.HALF_EVEN);
        selfPercent2 = new BigDecimal(selfSamples2).multiply(PERCENT_MULTIPLICAND).divide(total2, 2, RoundingMode.HALF_EVEN);
        totalPercent1 = new BigDecimal(totalSamples1).multiply(PERCENT_MULTIPLICAND).divide(total1, 2, RoundingMode.HALF_EVEN);
        totalPercent2 = new BigDecimal(totalSamples2).multiply(PERCENT_MULTIPLICAND).divide(total2, 2, RoundingMode.HALF_EVEN);
    }

    public BigDecimal getTotalDiff() {
        return totalPercent2.subtract(totalPercent1);
    }

    public BigDecimal getSelfDiff() {
        return selfPercent2.subtract(selfPercent1);
    }
}
