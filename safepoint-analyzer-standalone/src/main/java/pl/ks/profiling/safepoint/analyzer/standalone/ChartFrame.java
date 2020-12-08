package pl.ks.profiling.safepoint.analyzer.standalone;

import java.awt.HeadlessException;
import javax.swing.JFrame;
import javax.swing.JScrollPane;
import org.knowm.xchart.XChartPanel;
import org.knowm.xchart.internal.chartpart.Chart;

public class ChartFrame extends JFrame {
    public ChartFrame(Chart chart) throws HeadlessException {
        setSize(1500, 1000);
        setLocationRelativeTo(null);
        JScrollPane scrollBar = new JScrollPane(new XChartPanel<>(chart));
        add(scrollBar);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setVisible(true);
    }
}
