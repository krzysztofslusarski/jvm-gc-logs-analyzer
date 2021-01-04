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
import pl.ks.profiling.gui.commons.Page;
import pl.ks.profiling.safepoint.analyzer.commons.shared.JvmLogFile;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;
import java.util.function.Consumer;

class MenuPanel extends JPanel {
    private static final Color COLOR_LIGHT_GRAY = new Color(220, 220, 220);

    private final Consumer<Page> renderPage;
    private final Consumer<JvmLogFile> openGcLogsViewer;

    MenuPanel(JvmLogFile stats, Consumer<Page> renderPage, Consumer<JvmLogFile> openGcLogsViewer) {
        this.renderPage = renderPage;
        this.openGcLogsViewer = openGcLogsViewer;
        setBackground(COLOR_LIGHT_GRAY);
        setOpaque(true);
        setLayout(new MigLayout("", "[]10[]", "[]2[]"));
        setSize(300, 1000);
        addButtons(this, stats);
    }

    private void addButtons(Container container, JvmLogFile stats) {
        for (Page page : stats.getPages()) {
            container.add(createPageButton(page), "span");
        }
        container.add(createGcLogsViewerButton(stats), "span");
    }

    private JButton createGcLogsViewerButton(JvmLogFile stats) {
        JButton button = new JButton("GC logs viewer");
        button.setPreferredSize(new Dimension(280, 30));
        button.addActionListener(e -> openGcLogsViewer.accept(stats));
        return button;
    }

    private JButton createPageButton(Page page) {
        JButton button = new JButton(page.getMenuName());
        button.setPreferredSize(new Dimension(280, 25));
        button.addActionListener(renderPage(page));
        return button;
    }

    private ActionListener renderPage(Page page) {
        return e -> renderPage.accept(page);
    }
}
