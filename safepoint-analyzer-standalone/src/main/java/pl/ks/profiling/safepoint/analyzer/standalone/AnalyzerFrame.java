package pl.ks.profiling.safepoint.analyzer.standalone;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableColumnModel;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import net.miginfocom.swing.MigLayout;
import org.knowm.xchart.BitmapEncoder;
import org.knowm.xchart.PieChart;
import org.knowm.xchart.PieChartBuilder;
import org.knowm.xchart.XYChart;
import org.knowm.xchart.XYChartBuilder;
import org.knowm.xchart.XYSeries;
import org.knowm.xchart.internal.series.Series;
import org.knowm.xchart.style.Styler;
import org.knowm.xchart.style.lines.SeriesLines;
import org.knowm.xchart.style.markers.SeriesMarkers;
import pl.ks.profiling.gui.commons.Chart;
import pl.ks.profiling.gui.commons.Page;
import pl.ks.profiling.gui.commons.PageContent;
import pl.ks.profiling.gui.commons.Table;
import pl.ks.profiling.io.TempFileUtils;
import pl.ks.profiling.safepoint.analyzer.commons.shared.pareser.safepoint.SafepointLogFile;

public class AnalyzerFrame extends JFrame {
    private static final List<String> colors = List.of(
            "#3366cc", "#dc3912", "#ff9900", "#109618", "#990099", "#0099c6", "#dd4477", "#66aa00",
            "#b82e2e", "#316395", "#994499", "#22aa99", "#aaaa11", "#6633cc", "#e67300", "#8b0707",
            "#651067", "#329262", "#5574a6", "#3b3eac", "#b77322", "#16d620", "#b91383", "#f4359e",
            "#9c5935", "#a9c413", "#2a778d", "#668d1c", "#bea413", "#0c5922", "#743411"
    );
    public static final Color[] SERIES_COLORS = colors.stream()
            .map(Color::decode)
            .toArray(Color[]::new);

    private SafepointLogFile stats;
    private PresentationFontProvider presentationFontProvider;
    private AnalyzerFrame uberFrame = this;
    private ContentPanel contentPanel = new ContentPanel();
    private JScrollPane contentScroll = new JScrollPane(contentPanel);

    public AnalyzerFrame(SafepointLogFile stats, PresentationFontProvider presentationFontProvider) {
        this.stats = stats;
        this.presentationFontProvider = presentationFontProvider;

        setLayout(new BorderLayout());
        setTitle(stats.getFilename());
        setSize(1700, 1000);
        setLocationRelativeTo(null);

        contentScroll.getVerticalScrollBar().setUnitIncrement(32);
        add(new MenuPanel(), BorderLayout.LINE_START);
        add(contentScroll, BorderLayout.CENTER);

        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setVisible(true);
    }

    class MenuPanel extends JPanel {
        MenuPanel() {
            setBackground(new Color(220, 220, 220));
            setOpaque(true);
            setLayout(new MigLayout("", "[]10[]", "[]2[]"));
            setSize(300, 1000);
            for (Page page : stats.getPages()) {
                JButton button = new JButton(page.getMenuName());
                button.setPreferredSize(new Dimension(280, 25));
                button.addActionListener(e -> {
                    contentPanel.recreate(page);
                    contentScroll.getVerticalScrollBar().setValue(0);
                    contentScroll.getHorizontalScrollBar().setValue(0);
                    uberFrame.revalidate();
                    uberFrame.repaint();
                });
                add(button, "span");
            }
//                JButton button = new JButton("GC logs viewer");
//                button.setPreferredSize(new Dimension(280, 30));
//                button.addActionListener(e -> {
//                    new GcLogsViewerFrame(presentationFontProvider, stats.getGcLogFile());
//                });
//                add(button, "span");
        }
    }

    class ContentPanel extends JPanel {
        ContentPanel() {
            setLayout(new MigLayout());
            setBackground(Color.WHITE);
        }

        void recreate(Page page) {
            removeAll();
            JLabel pageTitle = new JLabel(page.getFullName(), JLabel.LEFT);
            pageTitle.setFont(presentationFontProvider.getDefaultH1Font());
            add(pageTitle, "span");
            for (PageContent pageContent : page.getPageContents()) {
                if (pageContent.getTitle() != null) {
                    JLabel title = new JLabel(pageContent.getTitle(), JLabel.LEFT);
                    title.setFont(presentationFontProvider.getDefaultH2Font());
                    add(title, "span");
                }
                if (pageContent.getInfo() != null) {
                    JLabel title = new JLabel("<html><div width=\"800px\">" + pageContent.getInfo() + "</div></html>");
                    title.setFont(presentationFontProvider.getDefaultFont());
                    add(title, "wrap");
                }
                switch (pageContent.getType()) {
                    case CHART:
                        addChartToPanel(pageContent);
//                        JButton showBig = new JButton("Show full width chart");
//                        add(showBig, "span");
//                        showBig.addActionListener(e -> {
//                            new ChartFrame(createFullWidthChart(pageContent));
//                        });
                        break;
                    case TABLE:
                        addTableToPanel(pageContent);
                        break;
                }
            }
        }

        private void addChartToPanel(PageContent pageContent) {
            Chart chart = (Chart) pageContent;
            String title = chart.getTitle() == null ? "" : chart.getTitle();
            String subTitle = pageContent.getTitle() == null ? "" : pageContent.getTitle().replaceAll("[^a-zA-Z0-9]", "");
            String filePath = TempFileUtils.getFilePath(subTitle + UUID.randomUUID().toString() + ".jpg");
            switch (chart.getChartType()) {
                case PIE:
                    PieChart pieChart = createPieChart(chart, title, 1200);
                    try {
                        BitmapEncoder.saveJPGWithQuality(pieChart, filePath, (float) 1);
                        JLabel label = new JLabel();
                        label.setIcon(new ImageIcon(filePath));
                        add(label, "span");
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    break;
                case POINTS_OR_LINE:
                case LINE:
                case POINTS:
                    XYChart xyChart = createXyChart(chart, title, 1200, chart.getChartType());
                    try {
                        BitmapEncoder.saveJPGWithQuality(xyChart, filePath, (float) 1);
                        JLabel label = new JLabel();
                        label.setIcon(new ImageIcon(filePath));
                        add(label, "span");
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    break;
            }
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
                    XYChart xyChart = createXyChart(chart, title, chart.getData().length + 500, chart.getChartType());
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

        private XYChart createXyChart(Chart chart, String title, int width, Chart.ChartType chartType) {
            XYChart xyChart = new XYChartBuilder()
                    .title(title)
                    .width(width)
                    .height(800)
                    .build();
            List<Number> xData = extractColumn(chart.getData(), 0, true);
            for (int i = 1; i < chart.getData()[0].length; i++) {
                XYSeries series = xyChart.addSeries(chart.getData()[0][i].toString(), xData, extractColumn(chart.getData(), i, true));
                if (chartType == Chart.ChartType.POINTS ||
                        (chartType == Chart.ChartType.POINTS_OR_LINE && chart.getSeriesTypes()[i - 1] == Chart.SeriesType.POINTS)) {
                    series.setMarker(SeriesMarkers.CIRCLE);
                    series.setLineStyle(SeriesLines.NONE);
                    xyChart.getStyler().setMarkerSize(3);
                } else {
                    series.setMarker(SeriesMarkers.NONE);
                }
            }

            if (chart.isForceZeroMinValue()) {
                xyChart.getStyler().setYAxisMin(0.0);
            }
            xyChart.getStyler().setDefaultSeriesRenderStyle(XYSeries.XYSeriesRenderStyle.Line);
            xyChart.getStyler().setYAxisLabelAlignment(Styler.TextAlignment.Right);
            xyChart.getStyler().setYAxisDecimalPattern("#,###.##");
            xyChart.getStyler().setLegendVisible(true);
            xyChart.getStyler().setChartBackgroundColor(Color.WHITE);
            xyChart.getStyler().setSeriesColors(SERIES_COLORS);
            xyChart.getStyler().setLegendFont(presentationFontProvider.getDefaultFont());
            xyChart.getStyler().setAxisTickLabelsFont(presentationFontProvider.getDefaultFont());
            xyChart.getStyler().setChartTitleFont(presentationFontProvider.getDefaultBoldFont());
            return xyChart;
        }

        private List<Number> extractColumn(Object[][] table, int column, boolean omitFirst) {
            List<Number> values = new ArrayList<>(table.length);
            int i = 0;
            for (Object[] objects : table) {
                if (omitFirst) {
                    omitFirst = false;
                    continue;
                }
                if (objects[column] == null) {
                    values.add(null);
                } else {
                    values.add(new BigDecimal(objects[column].toString()));
                }
            }
            return values;
        }


        private void addTableToPanel(PageContent pageContent) {
            Table content = (Table) pageContent;
            String[][] array = content.getTable().stream()
                    .map(line -> line.toArray(new String[0]))
                    .toArray(String[][]::new);
            String[] header = content.getHeader().toArray(new String[0]);
            JTable view = new JTable(array, header);
            view.setFillsViewportHeight(true);
            view.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
            update(view);
            JScrollPane tableScrollPane = new JScrollPane(view);
            add(tableScrollPane, "span");
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
}
