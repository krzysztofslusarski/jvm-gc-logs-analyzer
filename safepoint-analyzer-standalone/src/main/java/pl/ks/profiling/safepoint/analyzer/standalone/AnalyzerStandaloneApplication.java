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

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import pl.ks.profiling.io.InputUtils;
import pl.ks.profiling.safepoint.analyzer.commons.shared.JvmLogFile;
import pl.ks.profiling.safepoint.analyzer.commons.shared.StatsService;
import java.util.List;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;

@Slf4j
@SpringBootApplication
public class AnalyzerStandaloneApplication extends JFrame {
    @Autowired
    private StatsService statsService;

    @Autowired
    private PresentationFontProvider presentationFontProvider;

    public AnalyzerStandaloneApplication() {
    }

    public void init() throws ClassNotFoundException, UnsupportedLookAndFeelException, InstantiationException, IllegalAccessException {
        for (UIManager.LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
            if ("Nimbus".equals(info.getName())) {
                UIManager.setLookAndFeel(info.getClassName());
                break;
            }
        }
        UIManager.getLookAndFeelDefaults()
                .put("defaultFont", presentationFontProvider.getDefaultFont());

        JButton quitButton = new JButton("Quit");
        JButton loadButton = new JButton("Load file (JDK >= 9)");
        JButton loadOldButton = new JButton("Load file (JDK 8)");
        JButton concatLogsButton = new JButton("Concatenate rotated logs");

        loadButton.addActionListener((ActionEvent event) -> {
            try {
                JFileChooser fileChooser = new JFileChooser();
                int ret = fileChooser.showOpenDialog(null);
                if (ret == JFileChooser.APPROVE_OPTION) {
                    File file = fileChooser.getSelectedFile().getAbsoluteFile();
                    InputStream inputStream = InputUtils.getInputStream(file.getName(), file.getAbsolutePath());
                    JvmLogFile stats = statsService.createAllStatsUnifiedLogger(inputStream, file.getName());
                    new AnalyzerFrame(stats, presentationFontProvider);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        });

        loadOldButton.addActionListener((ActionEvent event) -> {
            try {
                JFileChooser fileChooser = new JFileChooser();
                int ret = fileChooser.showOpenDialog(null);
                if (ret == JFileChooser.APPROVE_OPTION) {
                    File file = fileChooser.getSelectedFile().getAbsoluteFile();
                    InputStream inputStream = InputUtils.getInputStream(file.getName(), file.getAbsolutePath());
                    JvmLogFile stats = statsService.createAllStatsJdk8(inputStream, file.getName());
                    new AnalyzerFrame(stats, presentationFontProvider);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        });

        concatLogsButton.addActionListener(this::onConcatLogsButtonClick);

        quitButton.addActionListener((ActionEvent event) -> {
            System.exit(0);
        });

        createLayout(loadButton, loadOldButton, concatLogsButton, quitButton);

        setTitle("Safepoint/GC log file analyzer");
        setSize(300, 200);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
    }

    private void onConcatLogsButtonClick(ActionEvent event) {
        File[] filesForConcatenation = pickFilesForConcatenation();
        if (filesForConcatenation != null) {
            List<File> sortedFiles = sortFilesByTimestamp(filesForConcatenation);
            if (sortedFiles != null) {
                File parent = sortedFiles.get(0).getParentFile();
                File saveFile = pickFileForConcatenationOutput(parent);
                if (saveFile != null) {
                    concatenateFiles(sortedFiles, saveFile);
                }
            }
        }
    }

    private File[] pickFilesForConcatenation() {
        JFileChooser filesToConcatenateChooser = new JFileChooser();
        filesToConcatenateChooser.setMultiSelectionEnabled(true);
        filesToConcatenateChooser.setDialogTitle("Select logs files for concatenation");
        int ret = filesToConcatenateChooser.showOpenDialog(null);
        if (ret == JFileChooser.APPROVE_OPTION) {
            return filesToConcatenateChooser.getSelectedFiles();
        } else {
            return null;
        }
    }

    private List<File> sortFilesByTimestamp(File[] files) {
        try {
            return FilesConcatenation.sortByTimestamp(files);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(null, "Error while checking files for concatenation", "Error", JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
            return null;
        }
    }

    private File pickFileForConcatenationOutput(File parentDirectory) {
        JFileChooser outputFileChooser = new JFileChooser();
        outputFileChooser.setSelectedFile(proposedFileForConcatenation(parentDirectory));
        int outputFileResult = outputFileChooser.showSaveDialog(null);
        if (outputFileResult == JFileChooser.APPROVE_OPTION) {
            return outputFileChooser.getSelectedFile();
        } else {
            return null;
        }
    }

    private File proposedFileForConcatenation(File parentDirectory) {
        return new File(parentDirectory.getAbsolutePath() + "/" + parentDirectory.getName() + ".combined.log");
    }

    private void concatenateFiles(List<File> sortedFiles, File saveFile) {
        try {
            FilesConcatenation.concatenate(sortedFiles, saveFile);
        } catch (IOException e) {
            JOptionPane.showMessageDialog(null, "Error while concatenating files", "Error", JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
        }
        JOptionPane.showMessageDialog(null, String.format("%d files has been successfully concatenated into %s", sortedFiles.size(), saveFile.getAbsolutePath()));
    }

    private void createLayout(JComponent... arg) {
        setLayout(new FlowLayout());
        for (JComponent jComponent : arg) {
            add(jComponent);
        }
    }

    public static void main(String[] args) {
        var ctx = new SpringApplicationBuilder(AnalyzerStandaloneApplication.class)
                .headless(false).run(args);

        EventQueue.invokeLater(() -> {
            var ex = ctx.getBean(AnalyzerStandaloneApplication.class);
            try {
                ex.init();
            } catch (Exception e) {
                e.printStackTrace();
            }
            ex.setVisible(true);
        });
    }
}
