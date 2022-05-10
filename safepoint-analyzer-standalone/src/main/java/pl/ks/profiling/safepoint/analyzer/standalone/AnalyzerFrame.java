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
import java.awt.*;

public class AnalyzerFrame extends JFrame {
    private final ContentPanel contentPanel;
    private final JScrollPane contentScroll;
    private final PresentationFontProviderStandalone presentationFontProvider;

    public AnalyzerFrame(JvmLogFile stats, PresentationFontProviderStandalone presentationFontProvider) {
        this.presentationFontProvider = presentationFontProvider;
        this.contentPanel = new ContentPanel(presentationFontProvider);
        this.contentScroll = new JScrollPane(contentPanel);

        setLayout(new BorderLayout());
        setTitle(stats.getParsing().getName());
        setSize(1700, 1000);
        setLocationRelativeTo(null);

        contentScroll.getVerticalScrollBar().setUnitIncrement(32);
        add(new MenuPanel(stats, this::renderPage, this::openGcLogsViewer), BorderLayout.LINE_START);
        add(contentScroll, BorderLayout.CENTER);

        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setVisible(true);
    }

    private void renderPage(Page page) {
        contentPanel.recreate(page);
        contentScroll.getVerticalScrollBar().setValue(0);
        contentScroll.getHorizontalScrollBar().setValue(0);
        this.revalidate();
        this.repaint();
    }

    private void openGcLogsViewer(JvmLogFile stats) {
        new GcLogsViewerFrame(presentationFontProvider, stats.getGcLogFile());
    }
}
