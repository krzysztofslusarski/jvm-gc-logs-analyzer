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
import pl.ks.profiling.io.source.LogsSource;
import pl.ks.profiling.safepoint.analyzer.commons.shared.ParserUtils;
import pl.ks.profiling.safepoint.analyzer.commons.shared.ParsingProgress;
import pl.ks.profiling.safepoint.analyzer.commons.shared.StatsService;
import pl.ks.profiling.safepoint.analyzer.commons.shared.report.JvmLogFile;

import com.formdev.flatlaf.FlatLightLaf;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.IOException;
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
    private PresentationFontProviderStandalone presentationFontProvider;

    private final String LOAD_BUTTON_LABEL = "Load file (JDK >= 9)";
    private final String LOAD_SHENANDOAH_BUTTON_LABEL = "Load file (Shenandoah GC)";
    private final String LOAD_ZGC_BUTTON_LABEL = "Load file (ZGC)";
    private final String LOAD_OLD_BUTTON_LABEL = "Load file (JDK 8)";
    private final String CONCAT_LOGS_BUTTON_LABEL = "Concatenate rotated logs";
    private JButton concatLogsButton;
    private JButton quitButton;
    private JButton loadButton;
    private JButton loadShenandoahButton;
    private JButton loadZgcButton;
    private JButton loadOldButton;
    private JLabel parsingProgressLabel;
    private final DecimalFormat TWO_DECIMAL_DIGITS_FORMAT = new DecimalFormat("##.#");
    private File lastDir;

    public AnalyzerStandaloneApplication() {
    }

    public void init() throws ClassNotFoundException, UnsupportedLookAndFeelException, InstantiationException, IllegalAccessException {
        FlatLightLaf.setup();
        UIManager.put("defaultFont", presentationFontProvider.getDefaultFont());
        UIManager.put("Button.arc", 8);
        UIManager.put("Component.arc", 8);
        UIManager.put("TextComponent.arc", 8);
        UIManager.put("ScrollBar.trackArc", 999);
        UIManager.put("ScrollBar.thumbArc", 999);
        UIManager.put("ScrollBar.width", 10);

        quitButton = new JButton("Quit");
        loadButton = new JButton(LOAD_BUTTON_LABEL);
        loadShenandoahButton = new JButton(LOAD_SHENANDOAH_BUTTON_LABEL);
        loadZgcButton = new JButton(LOAD_ZGC_BUTTON_LABEL);
        loadOldButton = new JButton(LOAD_OLD_BUTTON_LABEL);
        parsingProgressLabel = new JLabel("Parsing in progress. Processed xxx lines");
        parsingProgressLabel.setVisible(false);
        concatLogsButton = new JButton(CONCAT_LOGS_BUTTON_LABEL);

        loadButton.addActionListener(this::onLoadButtonClicked);
        loadShenandoahButton.addActionListener(this::onLoadShenandoahButtonClicked);
        loadZgcButton.addActionListener(this::onLoadZgcButtonClicked);
        loadOldButton.addActionListener(this::onLoadOldButtonClicked);
        concatLogsButton.addActionListener(this::onConcatLogsButtonClick);

        quitButton.addActionListener((ActionEvent event) -> {
            System.exit(0);
        });

        createLayout();

        setTitle("Safepoint/GC log file analyzer");
        pack();
        setSize(800, 600);
        setMinimumSize(getSize());
        setLocationRelativeTo(null);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
    }

    private void createLayout() {
        setLayout(new BorderLayout());

        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
        mainPanel.setBorder(new EmptyBorder(30, 50, 30, 50));

        JLabel titleLabel = new JLabel("GC Log Analyzer");
        titleLabel.setFont(presentationFontProvider.getDefaultH1Font());
        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        mainPanel.add(titleLabel);
        mainPanel.add(Box.createRigidArea(new Dimension(0, 8)));

        JLabel subtitleLabel = new JLabel("Safepoint & GC log file analysis tool");
        subtitleLabel.setFont(presentationFontProvider.getDefaultFont());
        subtitleLabel.setForeground(new Color(100, 100, 100));
        subtitleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        mainPanel.add(subtitleLabel);
        mainPanel.add(Box.createRigidArea(new Dimension(0, 24)));

        Dimension buttonSize = new Dimension(300, 36);
        styleButton(loadButton, buttonSize);
        styleButton(loadShenandoahButton, buttonSize);
        styleButton(loadZgcButton, buttonSize);
        styleButton(loadOldButton, buttonSize);
        styleButton(concatLogsButton, buttonSize);

        parsingProgressLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        mainPanel.add(loadButton);
        mainPanel.add(Box.createRigidArea(new Dimension(0, 8)));
        mainPanel.add(loadShenandoahButton);
        mainPanel.add(Box.createRigidArea(new Dimension(0, 8)));
        mainPanel.add(loadZgcButton);
        mainPanel.add(Box.createRigidArea(new Dimension(0, 8)));
        mainPanel.add(loadOldButton);
        mainPanel.add(Box.createRigidArea(new Dimension(0, 6)));
        mainPanel.add(parsingProgressLabel);
        mainPanel.add(Box.createRigidArea(new Dimension(0, 20)));

        JSeparator separator = new JSeparator();
        separator.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1));
        mainPanel.add(separator);
        mainPanel.add(Box.createRigidArea(new Dimension(0, 20)));
        mainPanel.add(concatLogsButton);

        add(mainPanel, BorderLayout.CENTER);

        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        bottomPanel.setBorder(new EmptyBorder(0, 0, 10, 0));
        quitButton.setPreferredSize(new Dimension(120, 32));
        bottomPanel.add(quitButton);
        add(bottomPanel, BorderLayout.PAGE_END);
    }

    private void styleButton(JButton button, Dimension size) {
        button.setAlignmentX(Component.CENTER_ALIGNMENT);
        button.setMaximumSize(size);
        button.setPreferredSize(size);
    }

    private void onLoadButtonClicked(ActionEvent event) {
        startLogsProcessing(statsService::createAllStatsUnifiedLogger);
    }

    private void onLoadShenandoahButtonClicked(ActionEvent event) {
        startLogsProcessing(statsService::createAllStatsShenandoah);
    }

    private void onLoadZgcButtonClicked(ActionEvent event) {
        startLogsProcessing(statsService::createAllStatsZgc);
    }

    private void onLoadOldButtonClicked(ActionEvent event) {
        startLogsProcessing(statsService::createAllStatsJdk8);
    }

    private void startLogsProcessing(ProcessLogs<LogsSource, Consumer<ParsingProgress>, Consumer<JvmLogFile>, JvmLogFile> logsProcessor) {
        processFilesForLogs(selectFilesForProcessing(), logsProcessor);
    }

    private void processFilesForLogs(List<File> files, ProcessLogs<LogsSource, Consumer<ParsingProgress>, Consumer<JvmLogFile>, JvmLogFile> logsProcessor) {
        if (files != null) {
            LogsSource logsSource;
            try {
                logsSource = InputUtils.getLogsSource(files, ParserUtils::getTimeStamp);
            } catch (IOException exception) {
                this.parsingFailed();
                throw new RuntimeException("Loading files for parsing has failed");
            }
            ParsingWorker worker = new ParsingWorker(logsSource, logsProcessor);
            LogsSource finalLogsSource = logsSource;
            worker.addPropertyChangeListener(
                    onWorkerComplete(
                            () -> this.parsingStarted(finalLogsSource),
                            this::successParsing,
                            this::parsingFailed));
            worker.execute();
        }
    }

    @AllArgsConstructor
    private class ParsingWorker extends SwingWorker<JvmLogFile, ParsingProgress> {
        private final LogsSource logsSource;
        private final ProcessLogs<LogsSource, Consumer<ParsingProgress>, Consumer<JvmLogFile>, JvmLogFile> logsProcessor;

        @Override
        protected JvmLogFile doInBackground() {
            try {
                JvmLogFile output = logsProcessor.apply(logsSource, this::publish, (JvmLogFile f) -> {
                });
                logsSource.close();
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

    private void parsingStarted(LogsSource logsSource) {
        loadButton.setEnabled(false);
        loadShenandoahButton.setEnabled(false);
        loadZgcButton.setEnabled(false);
        loadOldButton.setEnabled(false);
        notifyParsingChange(new ParsingProgress(0, false, logsSource.getTotalNumberOfFiles(), logsSource.getNumberOfFile(), 0));
        parsingProgressLabel.setVisible(true);
    }

    private void successParsing(JvmLogFile stats) {
        loadButton.setText(LOAD_BUTTON_LABEL);
        loadShenandoahButton.setText(LOAD_SHENANDOAH_BUTTON_LABEL);
        loadZgcButton.setText(LOAD_ZGC_BUTTON_LABEL);
        loadOldButton.setText(LOAD_OLD_BUTTON_LABEL);
        loadButton.setEnabled(true);
        loadShenandoahButton.setEnabled(true);
        loadZgcButton.setEnabled(true);
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
        loadShenandoahButton.setText(LOAD_SHENANDOAH_BUTTON_LABEL);
        loadZgcButton.setText(LOAD_ZGC_BUTTON_LABEL);
        loadOldButton.setText(LOAD_OLD_BUTTON_LABEL);
        loadButton.setEnabled(true);
        loadShenandoahButton.setEnabled(true);
        loadZgcButton.setEnabled(true);
        loadOldButton.setEnabled(true);
        JOptionPane.showMessageDialog(null, "Error while concatenating files", "Error", JOptionPane.ERROR_MESSAGE);
    }

    private List<File> selectFilesForProcessing() {
        JFileChooser fileChooser = new JFileChooser(lastDir);
        fileChooser.setDialogTitle("Select files with logs");
        fileChooser.setMultiSelectionEnabled(true);
        int ret = fileChooser.showOpenDialog(null);
        if (ret == JFileChooser.APPROVE_OPTION) {
            lastDir = fileChooser.getCurrentDirectory();
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
        parsingProgressLabel.setText(String.format(
                "Parsing in progress. \nParsing file %d of %d\n. Processed %d lines. (avg speed: %d lines per second)",
                progress.getCurrentFileNumber(),
                progress.getTotalFiles(),
                progress.getProcessedLines(),
                progress.getLinesPerSecond()));
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
            var statsService = ctx.getBean(StatsService.class);
            try {
                ex.init();
            } catch (Exception e) {
                e.printStackTrace();
            }
            ex.setVisible(true);
            if (args != null && args.length == 1) {
                log.info("Will try to process file from argument: {}", args[0]);
                ex.processFilesForLogs(List.of(new File(args[0])), statsService::createAllStatsUnifiedLogger);
            }
        });
    }
}
