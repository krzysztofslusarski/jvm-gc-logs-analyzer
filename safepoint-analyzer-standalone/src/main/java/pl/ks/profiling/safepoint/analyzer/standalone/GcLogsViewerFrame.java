package pl.ks.profiling.safepoint.analyzer.standalone;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.HeadlessException;
import java.util.List;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import net.miginfocom.swing.MigLayout;
import pl.ks.profiling.safepoint.analyzer.commons.shared.parser.gc.GcLogFile;

class GcLogsViewerFrame extends JFrame {
    private PresentationFontProvider presentationFontProvider;
    private Long collectionIdFrom = null;
    private Long collectionIdTo = null;
    private boolean showDecorators = false;
    private JScrollPane contentScroll;
    private GcLogsPanel gcLogsPanel;
    private GcLogsViewerFrame uberFrame;
    private GcLogFile gcLogFile;

    public GcLogsViewerFrame(PresentationFontProvider presentationFontProvider, GcLogFile gcLogFile) throws HeadlessException {
        this.presentationFontProvider = presentationFontProvider;
        this.gcLogsPanel = new GcLogsPanel();
        this.gcLogFile = gcLogFile;
        this.uberFrame = this;
        setLayout(new BorderLayout());
        setTitle("GC Logs Viewer");
        add(new GcLogsViewerPanel(), BorderLayout.NORTH);
        contentScroll = new JScrollPane(gcLogsPanel);
        contentScroll.getVerticalScrollBar().setUnitIncrement(32);
        add(contentScroll, BorderLayout.CENTER);
        setSize(1500, 1000);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setLocationRelativeTo(null);
        setVisible(true);
    }

    class GcLogsViewerPanel extends JPanel {
        public GcLogsViewerPanel() {
            setBackground(new Color(220, 220, 220));
            setOpaque(true);
            setLayout(new MigLayout());
            JLabel inputDescription = new JLabel("GC collection id:", JLabel.LEFT);
            inputDescription.setFont(presentationFontProvider.getDefaultFont());
            add(inputDescription);
            JTextField textField = new JTextField(20);
            add(textField);
            inputDescription = new JLabel("With decorators: ", JLabel.LEFT);
            inputDescription.setFont(presentationFontProvider.getDefaultFont());
            add(inputDescription);
            JCheckBox checkBox = new JCheckBox();
            add(checkBox);
            JButton button = new JButton("Show");
            button.addActionListener(e -> {
                String[] range = textField.getText().split("-");
                collectionIdFrom = Long.parseLong(range[0]);
                if (range.length > 1) {
                    collectionIdTo= Long.parseLong(range[1]);
                } else {
                    collectionIdTo = collectionIdFrom;
                }

                showDecorators = checkBox.isSelected();
                gcLogsPanel.recreate();
                contentScroll.getVerticalScrollBar().setValue(0);
                contentScroll.getHorizontalScrollBar().setValue(0);
                uberFrame.revalidate();
                uberFrame.repaint();
            });
            add(button, "span");
        }
    }

    class GcLogsPanel extends JPanel {
        public GcLogsPanel() {
            setBackground(Color.WHITE);
            setOpaque(true);
            setLayout(new MigLayout());
            showInfo();
        }

        private void showInfo() {
            JLabel inputDescription = new JLabel("Enter collection id to view log", JLabel.LEFT);
            inputDescription.setFont(presentationFontProvider.getDefaultFont());
            add(inputDescription);
        }

        void recreate() {
            removeAll();
            StringBuilder builder = new StringBuilder();
            for (long i = collectionIdFrom; i <= collectionIdTo; i++) {
                List<String> lines = gcLogFile.getLines().get(i);
                for (String line : lines) {
                    String toShow = showDecorators ? line : line.replaceFirst(".*GC\\(", "GC(");
                    builder.append(toShow).append("\n");
                }
            }

            if (builder.length() == 0) {
                showInfo();
                return;
            }

            JTextArea textArea = new JTextArea();
            textArea.setFont(presentationFontProvider.getMonospaceFont());
            textArea.setBorder(null);
            textArea.setEditable(false);

            textArea.setText(builder.toString());
            add(textArea);
        }
    }
}
