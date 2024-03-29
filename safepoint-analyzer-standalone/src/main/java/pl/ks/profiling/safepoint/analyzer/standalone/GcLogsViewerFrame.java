/*
 * Copyright 2020 Krzysztof Slusarski
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
import pl.ks.profiling.safepoint.analyzer.commons.shared.gc.parser.GCLogFile;

import javax.swing.*;
import java.awt.*;
import java.util.List;

class GcLogsViewerFrame extends JFrame {
    private PresentationFontProviderStandalone presentationFontProvider;
    private Long collectionIdFrom = null;
    private Long collectionIdTo = null;
    private boolean showDecorators = false;
    private JScrollPane contentScroll;
    private GcLogsPanel gcLogsPanel;
    private GcLogsViewerFrame uberFrame;
    private GCLogFile gcLogFile;

    public GcLogsViewerFrame(PresentationFontProviderStandalone presentationFontProvider, GCLogFile gcLogFile) throws HeadlessException {
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
                List<String> lines = gcLogFile.getRawLogLines().get(i);
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
