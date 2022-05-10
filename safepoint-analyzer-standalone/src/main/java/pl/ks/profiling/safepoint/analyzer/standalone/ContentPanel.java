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
import org.knowm.xchart.BitmapEncoder;
import org.knowm.xchart.PieChart;
import org.knowm.xchart.XYChart;
import org.knowm.xchart.internal.series.Series;
import org.knowm.xchart.style.Styler;
import pl.ks.profiling.gui.commons.Chart;
import pl.ks.profiling.gui.commons.Page;
import pl.ks.profiling.gui.commons.PageContent;
import pl.ks.profiling.gui.commons.Table;
import pl.ks.profiling.io.TempFileUtils;
import pl.ks.profiling.xchart.commons.XChartCreator;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableColumnModel;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import java.awt.*;
import java.util.UUID;

class ContentPanel extends JPanel {
    private final PresentationFontProviderStandalone presentationFontProvider;
    private final XChartCreator xChartCreator;

    ContentPanel(PresentationFontProviderStandalone presentationFontProvider) {
        this.presentationFontProvider = presentationFontProvider;
        setLayout(new MigLayout());
        setBackground(Color.WHITE);
        this.xChartCreator = new XChartCreator(presentationFontProvider);
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
                return chartToImage(tmpFilePath, xChartCreator.createPieChart(chart, title, 1200));
            case POINTS_OR_LINE:
            case LINE:
            case POINTS:
                return chartToImage(tmpFilePath, xChartCreator.createXyChart(chart, title, 1200));
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
                PieChart pieChart = xChartCreator.createPieChart(chart, title, 1200);
                return pieChart;
            case LINE:
            case POINTS:
                XYChart xyChart = xChartCreator.createXyChart(chart, title, chart.getData().length + 500);
                return xyChart;
        }
        return null;
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
