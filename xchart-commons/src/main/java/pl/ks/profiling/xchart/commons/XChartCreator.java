package pl.ks.profiling.xchart.commons;

import lombok.RequiredArgsConstructor;
import org.knowm.xchart.PieChart;
import org.knowm.xchart.PieChartBuilder;
import org.knowm.xchart.XYChart;
import org.knowm.xchart.XYChartBuilder;
import org.knowm.xchart.XYSeries;
import org.knowm.xchart.style.Styler;
import org.knowm.xchart.style.XYStyler;
import org.knowm.xchart.style.lines.SeriesLines;
import org.knowm.xchart.style.markers.SeriesMarkers;
import pl.ks.profiling.gui.commons.Chart;

import java.awt.*;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@RequiredArgsConstructor
public class XChartCreator {
    private static final List<String> COLORS = List.of(
            "#3366cc", "#dc3912", "#ff9900", "#109618", "#990099", "#0099c6", "#dd4477", "#66aa00",
            "#b82e2e", "#316395", "#994499", "#22aa99", "#aaaa11", "#6633cc", "#e67300", "#8b0707",
            "#651067", "#329262", "#5574a6", "#3b3eac", "#b77322", "#16d620", "#b91383", "#f4359e",
            "#9c5935", "#a9c413", "#2a778d", "#668d1c", "#bea413", "#0c5922", "#743411"
    );
    private static final Color[] SERIES_COLORS = COLORS.stream()
            .map(Color::decode)
            .toArray(Color[]::new);

    private final PresentationFontProvider presentationFontProvider;

    public PieChart createPieChart(Chart chart, String title, int width) {
        PieChart pieChart = createEmptyPieChart(title, width);
        for (Object[] row : chart.getRows()) {
            String seriesName = row[0].toString();
            BigDecimal value = new BigDecimal(row[1].toString());
            pieChart.addSeries(seriesName, value);
        }

        return pieChart;
    }

    private PieChart createEmptyPieChart(String title, int width) {
        PieChart pieChart = new PieChartBuilder().width(width).height(800).title(title).build();
        pieChart.getStyler().setSeriesColors(SERIES_COLORS);
        pieChart.getStyler().setLegendVisible(true);
        pieChart.getStyler().setChartBackgroundColor(Color.WHITE);
        pieChart.getStyler().setLegendFont(presentationFontProvider.getDefaultFont());
        pieChart.getStyler().setSumFont(presentationFontProvider.getDefaultFont());
        pieChart.getStyler().setChartTitleFont(presentationFontProvider.getDefaultBoldFont());
        return pieChart;
    }

    public XYChart createXyChart(Chart chart, String title, int width) {
        XYChart xyChart = createEmptyXyChart(chart, title, width);
        Object[][] chartRows = chart.getRows();
        java.util.List<Number> xAxis = extractColumnValues(chartRows, chart.getXAxisColumnIndex(), false);
        Object[] columnsHeadersRow = chart.getHeaders();

        for (int columnIndex = 0; columnIndex < columnsHeadersRow.length; columnIndex++) {
            if (columnIndex == chart.getXAxisColumnIndex()) {
                continue;
            }
            addSeriesForColumn(chart, chartRows, columnsHeadersRow, columnIndex, xAxis, xyChart);
        }
        return xyChart;
    }

    private void addSeriesForColumn(Chart chart, Object[][] rows, Object[] columnsHeadersRow, int columnIndex, java.util.List<Number> xAxis, XYChart xyChart) {
        String seriesName = columnsHeadersRow[columnIndex].toString();
        java.util.List<Number> seriesValues = extractColumnValues(rows, columnIndex, false);
        XYSeries series = xyChart.addSeries(seriesName, xAxis, seriesValues);
        if (chart.getChartType() == Chart.ChartType.POINTS ||
                (chart.getChartType() == Chart.ChartType.POINTS_OR_LINE && chart.getSeriesTypes()[columnIndex - 1] == Chart.SeriesType.POINTS)) {
            series.setMarker(SeriesMarkers.CIRCLE);
            series.setLineStyle(SeriesLines.NONE);
            xyChart.getStyler().setMarkerSize(3);
        } else {
            series.setMarker(SeriesMarkers.NONE);
        }
    }

    private XYChart createEmptyXyChart(Chart chart, String title, int width) {
        XYChart xyChart = new XYChartBuilder()
                .title(title)
                .xAxisTitle(chart.getXAxisLabel())
                .yAxisTitle(chart.getYAxisLabel())
                .width(width)
                .height(800)
                .build();
        XYStyler chartStyler = xyChart.getStyler();
        if (chart.isForceZeroMinValue()) {
            chartStyler.setYAxisMin(0.0);
        }
        chartStyler.setDefaultSeriesRenderStyle(XYSeries.XYSeriesRenderStyle.Line);
        chartStyler.setYAxisLabelAlignment(Styler.TextAlignment.Right);
        chartStyler.setYAxisDecimalPattern("#,###.##");
        chartStyler.setLegendVisible(true);
        chartStyler.setChartBackgroundColor(Color.WHITE);
        chartStyler.setSeriesColors(SERIES_COLORS);
        chartStyler.setLegendFont(presentationFontProvider.getDefaultFont());
        chartStyler.setAxisTickLabelsFont(presentationFontProvider.getDefaultFont());
        chartStyler.setChartTitleFont(presentationFontProvider.getDefaultBoldFont());
        return xyChart;
    }

    private java.util.List<Number> extractColumnValues(Object[][] table, int columnIndex, boolean skipHeader) {
        List<Number> values = new ArrayList<>(table.length);
        boolean skipNextRow = skipHeader;
        for (Object[] objects : table) {
            if (skipNextRow) {
                skipNextRow = false;
                continue;
            }
            if (objects[columnIndex] == null) {
                values.add(null);
            } else {
                values.add(new BigDecimal(objects[columnIndex].toString()));
            }
        }
        return values;
    }
}
