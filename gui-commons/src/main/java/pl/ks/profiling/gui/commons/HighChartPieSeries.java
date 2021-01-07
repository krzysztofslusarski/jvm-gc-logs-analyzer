package pl.ks.profiling.gui.commons;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class HighChartPieSeries {
    String name;
    HighChartPieData[] data;
}
