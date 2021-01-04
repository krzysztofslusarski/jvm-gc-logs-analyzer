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
package pl.ks.profiling.safepoint.analyzer.standalone;

import net.miginfocom.swing.MigLayout;
import org.knowm.xchart.*;
import org.knowm.xchart.internal.series.Series;
import org.knowm.xchart.style.Styler;
import org.knowm.xchart.style.XYStyler;
import org.knowm.xchart.style.lines.SeriesLines;
import org.knowm.xchart.style.markers.SeriesMarkers;
import pl.ks.profiling.gui.commons.Chart;
import pl.ks.profiling.gui.commons.Page;
import pl.ks.profiling.gui.commons.PageContent;
import pl.ks.profiling.gui.commons.Table;
import pl.ks.profiling.io.TempFileUtils;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableColumnModel;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import java.awt.*;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

class ContentPanel extends JPanel {
    private static final List<String> colors = List.of(
            "#3366cc", "#dc3912", "#ff9900", "#109618", "#990099", "#0099c6", "#dd4477", "#66aa00",
            "#b82e2e", "#316395", "#994499", "#22aa99", "#aaaa11", "#6633cc", "#e67300", "#8b0707",
            "#651067", "#329262", "#5574a6", "#3b3eac", "#b77322", "#16d620", "#b91383", "#f4359e",
            "#9c5935", "#a9c413", "#2a778d", "#668d1c", "#bea413", "#0c5922", "#743411"
    );
    private static final Color[] SERIES_COLORS = colors.stream()
            .map(Color::decode)
            .toArray(Color[]::new);

    private final PresentationFontProvider presentationFontProvider;

    ContentPanel(PresentationFontProvider presentationFontProvider) {
        this.presentationFontProvider = presentationFontProvider;
        setLayout(new MigLayout());
        setBackground(Color.WHITE);
    }

    void recreate(Page page) {
        removeAll();
        add(pageTitle(page), "span");
        for (PageContent pageContent : page.getPageContents()) {
            addPageContent(this, pageContent);
        }
    }

    private void addPageContent(Container container, PageContent pageContent) {
        if (pageContent.getTitle() != null) {
            container.add(contentTitle(pageContent), "span");
        }
        if (pageContent.getInfo() != null) {
            container.add(contentDescription(pageContent), "wrap");
        }
        switch (pageContent.getType()) {
            case CHART:
                add(createChart(pageContent), "span");
//                        JButton showBig = new JButton("Show full width chart");
//                        add(showBig, "span");
//                        showBig.addActionListener(e -> {
//                            new ChartFrame(createFullWidthChart(pageContent));
//                        });
                break;
            case TABLE:
                add(createTable(pageContent), "span");
                break;
        }
    }

    private JLabel contentDescription(PageContent pageContent) {
        JLabel title = new JLabel("<html><div width=\"800px\">" + pageContent.getInfo() + "</div></html>");
        title.setFont(presentationFontProvider.getDefaultFont());
        return title;
    }

    private JLabel contentTitle(PageContent pageContent) {
        return createTitle(pageContent.getTitle(), presentationFontProvider.getDefaultH2Font());
    }

    private JLabel pageTitle(Page page) {
        return createTitle(page.getFullName(), presentationFontProvider.getDefaultH1Font());
    }

    private JLabel createTitle(String fullName, Font defaultH1Font) {
        JLabel pageTitle = new JLabel(fullName, JLabel.LEFT);
        pageTitle.setFont(defaultH1Font);
        return pageTitle;
    }

    private JLabel createChart(PageContent pageContent) {
        Chart chart = (Chart) pageContent;
        String chartTitle = chart.getTitle();
        String title = chartTitle == null ? "" : chartTitle;
        String subTitle = chartTitle == null ? "" : chartTitle.replaceAll("[^a-zA-Z0-9]", "");
        String tmpFilePath = TempFileUtils.getFilePath(subTitle + UUID.randomUUID().toString() + ".jpg");
        switch (chart.getChartType()) {
            case PIE:
                return chartToImage(tmpFilePath, createPieChart(chart, title, 1200));
            case POINTS_OR_LINE:
            case LINE:
            case POINTS:
                return chartToImage(tmpFilePath, createXyChart(chart, title, 1200));
            default:
                return new JLabel("Unsupported chart type " + chart.getChartType());
        }
    }

    private <ST extends Styler, S extends Series> JLabel chartToImage(String filePath, org.knowm.xchart.internal.chartpart.Chart<ST, S> chart) {
        try {
            BitmapEncoder.saveJPGWithQuality(chart, filePath, (float) 1);
            return createImage(filePath);
        } catch (Exception e) {
            return new JLabel("Failed to render chart");
        }
    }

    private JLabel createImage(String filePath) {
        return new JLabel(new ImageIcon(filePath));
    }

    private org.knowm.xchart.internal.chartpart.Chart<? extends Styler, ? extends Series> createFullWidthChart(PageContent pageContent) {
        Chart chart = (Chart) pageContent;
        String title = chart.getTitle() == null ? "" : chart.getTitle();
        switch (chart.getChartType()) {
            case PIE:
                PieChart pieChart = createPieChart(chart, title, 1200);
                return pieChart;
            case LINE:
            case POINTS:
                XYChart xyChart = createXyChart(chart, title, chart.getData().length + 500);
                return xyChart;
        }
        return null;
    }

    private PieChart createPieChart(Chart chart, String title, int width) {
        boolean first = true;
        PieChart pieChart = new PieChartBuilder().width(width).height(800).title(title).build();
        for (Object[] data : chart.getData()) {
            if (first) {
                first = false;
                continue;
            }
            pieChart.addSeries(data[0].toString(), new BigDecimal(data[1].toString()));
        }
        pieChart.getStyler().setSeriesColors(SERIES_COLORS);
        pieChart.getStyler().setLegendVisible(true);
        pieChart.getStyler().setChartBackgroundColor(Color.WHITE);
        pieChart.getStyler().setLegendFont(presentationFontProvider.getDefaultFont());
        pieChart.getStyler().setSumFont(presentationFontProvider.getDefaultFont());
        pieChart.getStyler().setChartTitleFont(presentationFontProvider.getDefaultBoldFont());
        return pieChart;
    }

    private XYChart createXyChart(Chart chart, String title, int width) {
        XYChart xyChart = createEmptyXyChart(chart, title, width);
        Object[][] chartData = chart.getData();
        List<Number> xAxis = firstColumnValues(chartData);
        Object[] columnsHeadersRow = chartData[0];

        for (int columnIndex = 1; columnIndex < columnsHeadersRow.length; columnIndex++) {
            addSeriesForColumn(chart, chartData, columnsHeadersRow, columnIndex, xAxis, xyChart);
        }
        return xyChart;
    }

    private void addSeriesForColumn(Chart chart, Object[][] chartData, Object[] columnsHeadersRow, int columnIndex, List<Number> xAxis, XYChart xyChart) {
        String seriesName = columnsHeadersRow[columnIndex].toString();
        List<Number> columnValues = extractColumnValues(chartData, columnIndex, true);
        XYSeries series = xyChart.addSeries(seriesName, xAxis, columnValues);
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

    private List<Number> firstColumnValues(Object[][] chartData) {
        return extractColumnValues(chartData, 0, true);
    }

    private List<Number> extractColumnValues(Object[][] table, int columnIndex, boolean skipHeader) {
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


    private JScrollPane createTable(PageContent pageContent) {
        Table content = (Table) pageContent;
        String[][] array = content.getTable().stream()
                .map(line -> line.toArray(new String[0]))
                .toArray(String[][]::new);
        String[] header = content.getHeader().toArray(new String[0]);
        JTable view = new JTable(array, header);
        view.setFillsViewportHeight(true);
        view.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        update(view);
        return new JScrollPane(view);
    }

    private void update(JTable jTable) {
        adjustRowSizes(jTable);
        for (int i = 0; i < jTable.getColumnCount(); i++) {
            adjustColumnSizes(jTable, i, 2);
        }
        jTable.setPreferredScrollableViewportSize(jTable.getPreferredSize());
        for (int column = 1; column < jTable.getColumnCount(); column++) {
            DefaultTableCellRenderer rightRenderer = new DefaultTableCellRenderer();
            rightRenderer.setHorizontalAlignment(JLabel.RIGHT);
            jTable.getColumnModel().getColumn(column).setCellRenderer(rightRenderer);
        }
    }

    public void adjustColumnSizes(JTable table, int column, int margin) {
        DefaultTableColumnModel colModel = (DefaultTableColumnModel) table.getColumnModel();
        TableColumn col = colModel.getColumn(column);
        int width;

        TableCellRenderer renderer = col.getHeaderRenderer();
        if (renderer == null) {
            renderer = table.getTableHeader().getDefaultRenderer();
        }
        Component comp = renderer.getTableCellRendererComponent(table, col.getHeaderValue(), false, false, 0, 0);
        width = comp.getPreferredSize().width;

        for (int r = 0; r < table.getRowCount(); r++) {
            renderer = table.getCellRenderer(r, column);
            comp = renderer.getTableCellRendererComponent(table, table.getValueAt(r, column), false, false, r, column);
            int currentWidth = comp.getPreferredSize().width;
            width = Math.max(width, currentWidth);
        }

        width += 2 * margin;

        col.setPreferredWidth(width);
        col.setWidth(width);
    }

    private void adjustRowSizes(JTable jTable) {
        for (int row = 0; row < jTable.getRowCount(); row++) {
            int maxHeight = 0;
            for (int column = 0; column < jTable.getColumnCount(); column++) {
                TableCellRenderer cellRenderer = jTable.getCellRenderer(row, column);
                Object valueAt = jTable.getValueAt(row, column);
                Component tableCellRendererComponent = cellRenderer.getTableCellRendererComponent(jTable, valueAt, false, false, row, column);
                int heightPreferable = tableCellRendererComponent.getPreferredSize().height;
                maxHeight = Math.max(heightPreferable, maxHeight);
            }
            jTable.setRowHeight(row, maxHeight);
        }

    }
}
