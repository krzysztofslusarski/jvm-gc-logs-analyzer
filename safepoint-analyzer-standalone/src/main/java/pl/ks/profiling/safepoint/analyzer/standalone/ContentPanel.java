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
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableColumnModel;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import java.awt.*;
import java.util.UUID;

class ContentPanel extends JPanel {
    private static final Color ALT_ROW_COLOR = new Color(245, 247, 250);
    private static final Color TABLE_GRID_COLOR = new Color(228, 232, 238);

    private final PresentationFontProviderStandalone presentationFontProvider;
    private final XChartCreator xChartCreator;

    ContentPanel(PresentationFontProviderStandalone presentationFontProvider) {
        this.presentationFontProvider = presentationFontProvider;
        setLayout(new MigLayout("insets 20 24 20 24"));
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
        JLabel desc = new JLabel("<html><div width=\"800px\">" + pageContent.getInfo() + "</div></html>");
        desc.setFont(presentationFontProvider.getDefaultFont());
        desc.setForeground(new Color(80, 90, 100));
        return desc;
    }

    private JLabel contentTitle(PageContent pageContent) {
        JLabel title = createTitle(pageContent.getTitle(), presentationFontProvider.getDefaultH2Font());
        title.setBorder(new EmptyBorder(12, 0, 4, 0));
        return title;
    }

    private JLabel pageTitle(Page page) {
        JLabel title = createTitle(page.getFullName(), presentationFontProvider.getDefaultH1Font());
        title.setBorder(new EmptyBorder(0, 0, 8, 0));
        return title;
    }

    private JLabel createTitle(String fullName, Font font) {
        JLabel pageTitle = new JLabel(fullName, JLabel.LEFT);
        pageTitle.setFont(font);
        pageTitle.setForeground(new Color(30, 41, 59));
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
        view.setShowGrid(false);
        view.setIntercellSpacing(new Dimension(0, 0));
        view.setGridColor(TABLE_GRID_COLOR);
        view.setSelectionBackground(new Color(219, 234, 254));
        view.setSelectionForeground(new Color(30, 41, 59));
        view.getTableHeader().setBackground(new Color(248, 250, 252));
        view.getTableHeader().setForeground(new Color(71, 85, 105));
        view.getTableHeader().setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, TABLE_GRID_COLOR));
        update(view);
        JScrollPane scrollPane = new JScrollPane(view);
        scrollPane.setBorder(BorderFactory.createLineBorder(TABLE_GRID_COLOR));
        return scrollPane;
    }

    private void update(JTable jTable) {
        adjustRowSizes(jTable);
        for (int i = 0; i < jTable.getColumnCount(); i++) {
            adjustColumnSizes(jTable, i, 8);
        }
        jTable.setPreferredScrollableViewportSize(jTable.getPreferredSize());

        DefaultTableCellRenderer firstColRenderer = new AlternatingRowRenderer();
        firstColRenderer.setHorizontalAlignment(JLabel.LEFT);
        jTable.getColumnModel().getColumn(0).setCellRenderer(firstColRenderer);

        for (int column = 1; column < jTable.getColumnCount(); column++) {
            DefaultTableCellRenderer rightRenderer = new AlternatingRowRenderer();
            rightRenderer.setHorizontalAlignment(JLabel.RIGHT);
            jTable.getColumnModel().getColumn(column).setCellRenderer(rightRenderer);
        }
    }

    private static class AlternatingRowRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                boolean isSelected, boolean hasFocus, int row, int column) {
            Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            if (!isSelected) {
                c.setBackground(row % 2 == 0 ? Color.WHITE : ALT_ROW_COLOR);
            }
            setBorder(new EmptyBorder(4, 8, 4, 8));
            return c;
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
