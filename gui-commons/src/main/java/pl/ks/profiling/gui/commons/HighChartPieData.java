package pl.ks.profiling.gui.commons;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class HighChartPieData {
    String name;
    Object y;
}
