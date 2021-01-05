/*
 * Copyright 2020 Krzysztof Slusarski, Artur Owczarek
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
package pl.ks.profiling.gui.commons;

import lombok.Builder;
import lombok.Getter;
import lombok.Value;

import java.util.Arrays;

@Getter
@Value
@Builder
public class Chart implements PageContent {
    Object[][] data;
    ChartType chartType;
    SeriesType[] seriesTypes;
    String title;
    String info;
    String xAxisLabel;
    String yAxisLabel;
    boolean forceZeroMinValue;
    int xAxisColumnIndex;

    @Override
    public ContentType getType() {
        return ContentType.CHART;
    }

    public Object[][] getRows() {
        return Arrays.copyOfRange(data, 1, data.length - 2);
    }

    public Object[] getHeaders() {
        return data[0];
    }

    public String getXAxisLabel() {
        return emptyForNull(this.xAxisLabel);
    }

    public String getYAxisLabel() {
        return emptyForNull(this.yAxisLabel);
    }

    private String emptyForNull(String value) {
        if (value == null) {
            return "";
        }
        return value;
    }

    public enum ChartType {
        PIE,
        LINE,
        POINTS,
        POINTS_OR_LINE,
    }

    public enum SeriesType {
        LINE,
        POINTS,
    }
}
