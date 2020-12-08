package pl.ks.profiling.gui.commons;

import lombok.Builder;
import lombok.Getter;
import lombok.Value;

@Getter
@Value
@Builder
public class Chart implements PageContent {
    Object[][] data;
    ChartType chartType;
    SeriesType[] seriesTypes;
    String title;
    String info;
    boolean forceZeroMinValue;

    @Override
    public ContentType getType() {
        return ContentType.CHART;
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
