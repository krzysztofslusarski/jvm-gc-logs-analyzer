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
import pl.ks.profiling.safepoint.analyzer.commons.shared.ParsingProgress;
import pl.ks.profiling.safepoint.analyzer.commons.shared.JvmLogFile;
import pl.ks.profiling.safepoint.analyzer.commons.shared.ParserUtils;
import pl.ks.profiling.safepoint.analyzer.commons.shared.StatsService;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
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
import java.util.function.Consumer;
import java.util.function.Function;

@Slf4j
@SpringBootApplication
public class AnalyzerStandaloneApplication extends JFrame {
    @Autowired
    private StatsService statsService;

    @Autowired
    private PresentationFontProvider presentationFontProvider;

    private final String LOAD_BUTTON_LABEL = "Load file (JDK >= 9)";
    private final String LOAD_OLD_BUTTON_LABEL = "Load file (JDK 8)";
    private final String CONCAT_LOGS_BUTTON_LABEL = "Concatenate rotated logs";
    private JButton concatLogsButton;
    private JButton quitButton;
    private JButton loadButton;
    private JButton loadOldButton;
    private JLabel parsingProgressLabel;
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
        loadButton = new JButton(LOAD_BUTTON_LABEL);
        loadOldButton = new JButton(LOAD_OLD_BUTTON_LABEL);
        parsingProgressLabel = new JLabel("Parsing in progress. Processed xxx lines");
        parsingProgressLabel.setVisible(false);
        concatLogsButton = new JButton(CONCAT_LOGS_BUTTON_LABEL);

        loadButton.addActionListener(this::onLoadButtonClicked);
        loadOldButton.addActionListener(this::onLoadOldButtonClicked);
        concatLogsButton.addActionListener(this::onConcatLogsButtonClick);

        quitButton.addActionListener((ActionEvent event) -> {
            System.exit(0);
        });

        createLayout();

        setTitle("Safepoint/GC log file analyzer");
        pack();
        setMinimumSize(getSize());
        setLocationRelativeTo(null);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
    }

    private void createLayout() {
        setLayout(new BorderLayout());

        JPanel buttonsPanel = new JPanel();
        buttonsPanel.setLayout(new BoxLayout(buttonsPanel, BoxLayout.Y_AXIS));
        buttonsPanel.setBorder(new EmptyBorder(10, 30, 10, 30));
        loadButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        loadOldButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        parsingProgressLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        concatLogsButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        buttonsPanel.add(loadButton);
        buttonsPanel.add(loadOldButton);
        buttonsPanel.add(parsingProgressLabel);
        buttonsPanel.add(Box.createRigidArea(new Dimension(0,10)));
        buttonsPanel.add(concatLogsButton);

        add(buttonsPanel, BorderLayout.CENTER);
        add(quitButton, BorderLayout.PAGE_END);
    }

    private void onLoadButtonClicked(ActionEvent event) {
        startLogsProcessing(statsService::createAllStatsUnifiedLogger);
    }

    private void onLoadOldButtonClicked(ActionEvent event) {
        startLogsProcessing(statsService::createAllStatsJdk8);
    }

    private void startLogsProcessing(TriFunction<InputStream, String, Consumer<ParsingProgress>, JvmLogFile> logsProcessor) {
        processFilesForLogs(selectFilesForProcessing(), logsProcessor);
    }

    private void processFilesForLogs(List<File> files, TriFunction<InputStream, String, Consumer<ParsingProgress>, JvmLogFile> logsProcessor) {
        if (files != null) {
            ParsingWorker worker = new ParsingWorker(files, logsProcessor);
            worker.addPropertyChangeListener(
                    onWorkerComplete(
                            this::parsingStarted,
                            this::successParsing,
                            this::parsingFailed));
            worker.execute();
        }
    }

    @AllArgsConstructor
    private class ParsingWorker extends SwingWorker<JvmLogFile, ParsingProgress> {
        private final List<File> files;
        private final TriFunction<InputStream, String, Consumer<ParsingProgress>, JvmLogFile> logsProcessor;
        @Override
        protected JvmLogFile doInBackground() {
            try {
                InputStream logsInputStream = InputUtils.getInputStream(files, ParserUtils::getTimeStamp);
                JvmLogFile output = logsProcessor.apply(logsInputStream, files.get(0).getName(), this::publish);
                logsInputStream.close();
                return output;
            } catch (IOException exception) {
                exception.printStackTrace();
            }
            return null;
        }

        @Override
        protected void process(List<ParsingProgress> chunks) {
            notifyParsingChange(chunks.get(chunks.size() - 1));
        }
    }

    private void parsingStarted() {
        loadButton.setEnabled(false);
        loadOldButton.setEnabled(false);
        notifyParsingChange(new ParsingProgress(0));
        parsingProgressLabel.setVisible(true);
    }

    private void successParsing(JvmLogFile stats) {
        loadButton.setText(LOAD_BUTTON_LABEL);
        loadOldButton.setText(LOAD_OLD_BUTTON_LABEL);
        loadButton.setEnabled(true);
        loadOldButton.setEnabled(true);
        parsingProgressLabel.setVisible(false);
        if (stats != null) {
            new AnalyzerFrame(stats, presentationFontProvider);
        } else {
            JOptionPane.showMessageDialog(null, "Error while loading files with logs", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void parsingFailed() {
        loadButton.setText(LOAD_BUTTON_LABEL);
        loadOldButton.setText(LOAD_OLD_BUTTON_LABEL);
        loadButton.setEnabled(true);
        loadOldButton.setEnabled(true);
        JOptionPane.showMessageDialog(null, "Error while concatenating files", "Error", JOptionPane.ERROR_MESSAGE);
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
                        (success) -> successConcatenation(sortedFiles, saveFile),
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

    private <T> PropertyChangeListener onWorkerComplete(Runnable started, Consumer<T> success, Runnable failure) {
        return (PropertyChangeEvent evt) -> {
            if ("state".equals(evt.getPropertyName())) {
                switch ((SwingWorker.StateValue) evt.getNewValue()) {
                    case STARTED:
                        started.run();
                        break;
                    case DONE:
                        try {
                            T jobSucceeded = ((SwingWorker<T, ?>) evt.getSource()).get();
                            if (jobSucceeded != null) {
                                success.accept(jobSucceeded);
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

    private void notifyParsingChange(ParsingProgress progress) {
        parsingProgressLabel.setText("Parsing in progress. Processed " + progress.getProcessedLines() + " lines...");
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
