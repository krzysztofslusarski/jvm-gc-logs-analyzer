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

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import pl.ks.profiling.io.ConcatenationProgress;
import pl.ks.profiling.io.FilesConcatenation;
import pl.ks.profiling.io.InputUtils;
import pl.ks.profiling.safepoint.analyzer.commons.shared.JvmLogFile;
import pl.ks.profiling.safepoint.analyzer.commons.shared.ParserUtils;
import pl.ks.profiling.safepoint.analyzer.commons.shared.StatsService;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.text.DecimalFormat;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.function.BiFunction;
import java.util.function.Function;

@Slf4j
@SpringBootApplication
public class AnalyzerStandaloneApplication extends JFrame {
    @Autowired
    private StatsService statsService;

    @Autowired
    private PresentationFontProvider presentationFontProvider;

    private JButton concatLogsButton;
    private JButton quitButton;
    private JButton loadButton;
    private JButton loadOldButton;
    private final String CONCAT_LOGS_BUTTON_LABEL = "Concatenate rotated logs";
    private final DecimalFormat TWO_DECIMAL_DIGITS_FORMAT = new DecimalFormat("##.#");

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

        quitButton = new JButton("Quit");
        loadButton = new JButton("Load file (JDK >= 9)");
        loadOldButton = new JButton("Load file (JDK 8)");
        concatLogsButton = new JButton(CONCAT_LOGS_BUTTON_LABEL);

        loadButton.addActionListener(this::onLoadButtonClicked);
        loadOldButton.addActionListener(this::onLoadOldButtonClicked);
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

    private void onLoadButtonClicked(ActionEvent event) {
        JvmLogFile stats = startLogsProcessing(statsService::createAllStatsUnifiedLogger);
        if (stats != null) {
            new AnalyzerFrame(stats, presentationFontProvider);
        } else {
            JOptionPane.showMessageDialog(null, "Error while loading files with logs", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void onLoadOldButtonClicked(ActionEvent event) {
        JvmLogFile stats = startLogsProcessing(statsService::createAllStatsJdk8);
        if (stats != null) {
            new AnalyzerFrame(stats, presentationFontProvider);
        } else {
            JOptionPane.showMessageDialog(null, "Error while loading files with JVM 8 logs", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private JvmLogFile startLogsProcessing(BiFunction<InputStream, String, JvmLogFile> logsProcessorr) {
        List<File> files = selectFilesForProcessing();
        return processFilesForLogs(files, logsProcessorr);
    }

    private JvmLogFile processFilesForLogs(List<File> files, BiFunction<InputStream, String, JvmLogFile> logsProcessor) {
        if (files != null) {
            try {
                InputStream logsInputStream = InputUtils.getInputStream(files, ParserUtils::getTimeStamp);
                JvmLogFile output = logsProcessor.apply(logsInputStream, files.get(0).getName());
                logsInputStream.close();
                return output;
            } catch (IOException exception) {
                exception.printStackTrace();
            }
        }
        return null;
    }

    private List<File> selectFilesForProcessing() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Select files with logs");
        fileChooser.setMultiSelectionEnabled(true);
        int ret = fileChooser.showOpenDialog(null);
        if (ret == JFileChooser.APPROVE_OPTION) {
            return Arrays.asList(fileChooser.getSelectedFiles());
        } else {
            return null;
        }
    }

    private void onConcatLogsButtonClick(ActionEvent event) {
        File[] filesForConcatenation = pickFilesForConcatenation();
        if (filesForConcatenation != null) {
            List<File> sortedFiles = sortFilesByTimestamp(Arrays.asList(filesForConcatenation));
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

    private List<File> sortFilesByTimestamp(List<File> files) {
        try {
            return FilesConcatenation.sortBy(files, f -> firstLineExtractor(f, ParserUtils::getTimeStamp));
        } catch (Exception e) {
            JOptionPane.showMessageDialog(null, "Error while checking files for concatenation", "Error", JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
            return null;
        }
    }

    static <U extends Comparable<? super U>> U firstLineExtractor(File file, Function<String, U> extractCompareObject) {
        try {
            return Files.lines(file.toPath()).map(extractCompareObject).findFirst().get();
        } catch (IOException exception) {
            throw new RuntimeException(exception);
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

    @AllArgsConstructor
    private class ConcatenatingWorker extends SwingWorker<Boolean, ConcatenationProgress> {
        private final List<java.io.File> sortedFiles;
        private final File saveFile;

        @Override
        protected Boolean doInBackground() throws Exception {
            FilesConcatenation.concatenate(sortedFiles, saveFile, this::publish);
            return true;
        }

        @Override
        protected void process(List<ConcatenationProgress> chunks) {
            notifyConcatenationChange(chunks.get(chunks.size() - 1));
        }
    }


    private void concatenateFiles(List<File> sortedFiles, File saveFile) {
        ConcatenatingWorker concatenatingWorker = new ConcatenatingWorker(sortedFiles, saveFile);
        concatenatingWorker.addPropertyChangeListener(
                onWorkerComplete(
                        this::concatenationStarted,
                        () -> successConcatenation(sortedFiles, saveFile),
                        this::concatenationFailed));
        concatenatingWorker.execute();
    }

    private void concatenationStarted() {
        concatLogsButton.setEnabled(false);
    }

    private void successConcatenation(List<File> sortedFiles, File saveFile) {
        JOptionPane.showMessageDialog(null, String.format("%d files has been successfully concatenated into %s", sortedFiles.size(), saveFile.getAbsolutePath()));
        concatLogsButton.setText(CONCAT_LOGS_BUTTON_LABEL);
        concatLogsButton.setEnabled(true);
    }

    private void concatenationFailed() {
        concatLogsButton.setText(CONCAT_LOGS_BUTTON_LABEL);
        concatLogsButton.setEnabled(true);
        JOptionPane.showMessageDialog(null, "Error while concatenating files", "Error", JOptionPane.ERROR_MESSAGE);
    }

    private PropertyChangeListener onWorkerComplete(Runnable started, Runnable success, Runnable failure) {
        return (PropertyChangeEvent evt) -> {
            if ("state".equals(evt.getPropertyName())) {
                switch ((SwingWorker.StateValue) evt.getNewValue()) {
                    case STARTED:
                        started.run();
                        break;
                    case DONE:
                        try {
                            Boolean jobSucceeded = ((ConcatenatingWorker) evt.getSource()).get();
                            if (jobSucceeded) {
                                success.run();
                            } else {
                                failure.run();
                            }
                        } catch (InterruptedException | ExecutionException e) {
                            e.printStackTrace();
                            failure.run();
                        }
                        break;
                }
            }
        };
    }

    private void notifyConcatenationChange(ConcatenationProgress progress) {
        String response = String.format("Processing file %d/%d. %s %s",
                progress.getCurrentFileNumber(),
                progress.getNumberOfFiles(),
                percent(progress.getAllFilesSizeBytes(), progress.getProcessedBytes()),
                formatProcessedBytes(progress.getAllFilesSizeBytes(), progress.getProcessedBytes()));
        concatLogsButton.setText(response);
    }

    private String percent(Long total, Long share) {
        return TWO_DECIMAL_DIGITS_FORMAT.format((share.doubleValue() / total.doubleValue()) * 100.0) + "%";
    }

    private String formatProcessedBytes(Long total, Long share) {
        return toMb(share) + "/" + toMb(total);
    }

    private String toMb(Long bytes) {
        return TWO_DECIMAL_DIGITS_FORMAT.format(bytes.doubleValue() / 1024 / 1024) + "Mb";
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
