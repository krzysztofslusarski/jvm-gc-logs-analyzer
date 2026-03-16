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

import pl.ks.profiling.gui.commons.Page;
import pl.ks.profiling.safepoint.analyzer.commons.shared.report.JvmLogFile;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionListener;
import java.util.function.Consumer;

class MenuPanel extends JPanel {
    private static final Color SIDEBAR_BG = new Color(245, 247, 250);
    private static final Color SIDEBAR_BORDER = new Color(218, 222, 230);
    private static final Color HOVER_BG = new Color(232, 236, 242);
    private static final Color SELECTED_BG = new Color(219, 234, 254);
    private static final Color SELECTED_FG = new Color(30, 64, 175);
    private static final Dimension BUTTON_SIZE = new Dimension(260, 32);

    private final Consumer<Page> renderPage;
    private final Consumer<JvmLogFile> openGcLogsViewer;
    private JButton selectedButton = null;

    MenuPanel(JvmLogFile stats, Consumer<Page> renderPage, Consumer<JvmLogFile> openGcLogsViewer) {
        this.renderPage = renderPage;
        this.openGcLogsViewer = openGcLogsViewer;
        setLayout(new BorderLayout());
        setBackground(SIDEBAR_BG);
        setBorder(BorderFactory.createMatteBorder(0, 0, 0, 1, SIDEBAR_BORDER));

        JPanel buttonList = new JPanel();
        buttonList.setLayout(new BoxLayout(buttonList, BoxLayout.Y_AXIS));
        buttonList.setBackground(SIDEBAR_BG);
        buttonList.setBorder(new EmptyBorder(12, 10, 12, 10));

        JLabel navLabel = new JLabel("Navigation");
        navLabel.setFont(navLabel.getFont().deriveFont(Font.BOLD, 11f));
        navLabel.setForeground(new Color(130, 140, 155));
        navLabel.setBorder(new EmptyBorder(0, 8, 8, 0));
        navLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        buttonList.add(navLabel);

        for (Page page : stats.getPages()) {
            buttonList.add(createPageButton(page));
            buttonList.add(Box.createRigidArea(new Dimension(0, 2)));
        }

        buttonList.add(Box.createRigidArea(new Dimension(0, 10)));
        JSeparator sep = new JSeparator();
        sep.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1));
        buttonList.add(sep);
        buttonList.add(Box.createRigidArea(new Dimension(0, 10)));

        JLabel toolsLabel = new JLabel("Tools");
        toolsLabel.setFont(toolsLabel.getFont().deriveFont(Font.BOLD, 11f));
        toolsLabel.setForeground(new Color(130, 140, 155));
        toolsLabel.setBorder(new EmptyBorder(0, 8, 8, 0));
        toolsLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        buttonList.add(toolsLabel);
        buttonList.add(createGcLogsViewerButton(stats));

        JScrollPane scrollPane = new JScrollPane(buttonList);
        scrollPane.setBorder(null);
        scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        add(scrollPane, BorderLayout.CENTER);
    }

    private JButton createGcLogsViewerButton(JvmLogFile stats) {
        JButton button = createStyledButton("GC logs viewer");
        button.addActionListener(e -> openGcLogsViewer.accept(stats));
        return button;
    }

    private JButton createPageButton(Page page) {
        JButton button = createStyledButton(page.getMenuName());
        button.addActionListener(renderPage(page));
        return button;
    }

    private JButton createStyledButton(String text) {
        JButton button = new JButton(text);
        button.setMaximumSize(BUTTON_SIZE);
        button.setPreferredSize(BUTTON_SIZE);
        button.setAlignmentX(Component.LEFT_ALIGNMENT);
        button.setHorizontalAlignment(SwingConstants.LEFT);
        button.setFocusPainted(false);
        button.setBorderPainted(false);
        button.setContentAreaFilled(false);
        button.setOpaque(true);
        button.setBackground(SIDEBAR_BG);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        button.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseEntered(java.awt.event.MouseEvent e) {
                if (button != selectedButton) {
                    button.setBackground(HOVER_BG);
                }
            }
            @Override
            public void mouseExited(java.awt.event.MouseEvent e) {
                if (button != selectedButton) {
                    button.setBackground(SIDEBAR_BG);
                }
            }
        });

        button.addActionListener(e -> {
            if (selectedButton != null) {
                selectedButton.setBackground(SIDEBAR_BG);
                selectedButton.setForeground(UIManager.getColor("Button.foreground"));
            }
            selectedButton = button;
            button.setBackground(SELECTED_BG);
            button.setForeground(SELECTED_FG);
        });

        return button;
    }

    private ActionListener renderPage(Page page) {
        return e -> renderPage.accept(page);
    }
}
