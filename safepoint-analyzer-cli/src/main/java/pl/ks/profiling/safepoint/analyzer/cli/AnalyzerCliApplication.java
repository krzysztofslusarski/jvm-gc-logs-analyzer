package pl.ks.profiling.safepoint.analyzer.cli;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.knowm.xchart.BitmapEncoder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import pl.ks.profiling.gui.commons.Chart;
import pl.ks.profiling.gui.commons.Page;
import pl.ks.profiling.gui.commons.PageContent;
import pl.ks.profiling.gui.commons.Table;
import pl.ks.profiling.io.InputUtils;
import pl.ks.profiling.io.source.LogsSource;
import pl.ks.profiling.safepoint.analyzer.commons.shared.ParserUtils;
import pl.ks.profiling.safepoint.analyzer.commons.shared.StatsService;
import pl.ks.profiling.safepoint.analyzer.commons.shared.report.JvmLogFile;
import pl.ks.profiling.xchart.commons.PresentationFontProvider;
import pl.ks.profiling.xchart.commons.XChartCreator;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.List;

@Slf4j
@SpringBootApplication
public class AnalyzerCliApplication {
    private static final String REPORT_DIR = "report";

    private final PresentationFontProvider presentationFontProvider = new PresentationFontProviderCli();
    private final XChartCreator xChartCreator = new XChartCreator(presentationFontProvider);

    @Autowired
    private StatsService statsService;

    private int imageIndex;

    void run(String[] args) throws IOException {
        var resultDir = Paths.get(REPORT_DIR);
        if (Files.exists(resultDir)) {
            throw new IllegalStateException("The report dir already exists");
        }

        LogsSource logsSource = InputUtils.getLogsSource(List.of(new File(args[0])), ParserUtils::getTimeStamp);
        statsService.createAllStatsUnifiedLogger(logsSource, parsingProgress -> {
        }, jvmLogFile -> {
            createReport(jvmLogFile, resultDir);
        });
    }

    @SneakyThrows
    private void createReport(JvmLogFile jvmLogFile, Path resultDir) {
        Files.createDirectory(resultDir);
        Path htmlReportFile = resultDir.resolve("report.html");
        try (OutputStream outputStream = Files.newOutputStream(htmlReportFile, StandardOpenOption.CREATE);
             OutputStreamWriter outputStreamWriter = new OutputStreamWriter(outputStream)) {
            outputStreamWriter.write("<html>\n");
            outputStreamWriter.write("<head>\n");
            outputStreamWriter.write("    <meta charset=\"utf-8\">\n");
            outputStreamWriter.write("    <meta name=\"viewport\" content=\"width=device-width, initial-scale=1, shrink-to-fit=no\">\n");
            outputStreamWriter.write("    <link rel=\"stylesheet\" href=\"https://cdnjs.cloudflare.com/ajax/libs/materialize/1.0.0/css/materialize.min.css\">\n");
            outputStreamWriter.write("</head>\n");
            outputStreamWriter.write("<body>\n");
            outputStreamWriter.write("</body>\n");

            outputStreamWriter.write("<div class=\"container\">\n");
            for (Page page : jvmLogFile.getPages()) {
                createReport(page, outputStreamWriter, resultDir);
            }
            outputStreamWriter.write("</div>\n");
            outputStreamWriter.write("</html>\n");
        }
    }

    private void createReport(Page page, OutputStreamWriter outputStreamWriter, Path resultDir) throws IOException {
        outputStreamWriter.write("<div class=\"row\">\n");
        outputStreamWriter.write("<div class=\"col s12\">\n");
        outputStreamWriter.write("<div class=\"card\">\n");
        outputStreamWriter.write("<div class=\"card-content\">\n");
        outputStreamWriter.write("<span class=\"card-title\">\n");
        outputStreamWriter.write(page.getFullName());
        outputStreamWriter.write("</span>\n");
        if (page.getInfo() != null) {
            outputStreamWriter.write("<p>\n");
            outputStreamWriter.write(page.getInfo());
            outputStreamWriter.write("</p>\n");
        }
        for (PageContent pageContent : page.getPageContents()) {
            createReport(pageContent, outputStreamWriter, resultDir);
        }
        outputStreamWriter.write("</div>\n");
        outputStreamWriter.write("</div>\n");
        outputStreamWriter.write("</div>\n");
        outputStreamWriter.write("</div>\n");
    }

    @SneakyThrows
    private void createReport(PageContent pageContent, OutputStreamWriter outputStreamWriter, Path resultDir) {
        outputStreamWriter.write("<div class=\"row\">\n");
        outputStreamWriter.write("<div class=\"col s12\">\n");
        switch (pageContent.getType()) {
            case CHART:
                Chart chart = (Chart) pageContent;
                String chartTitle = chart.getTitle();
                String title = chartTitle == null ? "" : chartTitle;
                org.knowm.xchart.internal.chartpart.Chart xChart = null;
                switch (chart.getChartType()) {
                    case PIE:
                         xChart = xChartCreator.createPieChart(chart, title, 1200);
                        break;
                    case LINE:
                    case POINTS:
                    case POINTS_OR_LINE:
                        xChart = xChartCreator.createXyChart(chart, title, 1200);
                        break;
                }
                String filename = resultDir.resolve(++imageIndex + ".jpg").toAbsolutePath().toString();
                BitmapEncoder.saveJPGWithQuality(xChart, filename, (float) 1);
                outputStreamWriter.write("<img src=\"" + imageIndex+ ".jpg\"/><br/>\n");
                break;
            case TABLE:
                outputStreamWriter.write("<table>\n");
                outputStreamWriter.write("<thead>\n");
                Table table = (Table) pageContent;
                for (String header : table.getHeader()) {
                    outputStreamWriter.write("<th>\n");
                    outputStreamWriter.write(header);
                    outputStreamWriter.write("</th>\n");
                }
                outputStreamWriter.write("</thead>\n");
                outputStreamWriter.write("<tbody>\n");
                for (List<String> row : table.getTable()) {
                    outputStreamWriter.write("<tr>\n");
                    for (String cell : row) {
                        outputStreamWriter.write("<td>\n");
                        outputStreamWriter.write(cell);
                        outputStreamWriter.write("</td>\n");
                    }
                    outputStreamWriter.write("</tr>\n");
                }
                outputStreamWriter.write("</tbody>\n");
                outputStreamWriter.write("</table>\n");

                break;
        }
        outputStreamWriter.write("</div>\n");
        outputStreamWriter.write("</div>\n");
    }

    private static void printInfo() {
        System.out.println("Proper usage:");
        System.out.println("  java -jar analyzer-cli.jar <jvm log file>");
        System.out.println("The report will be created in " + REPORT_DIR + " directory. If that directory exists the IllegalStateException is thrown.");
    }

    public static void main(String[] args) throws Exception {
        if (args.length != 1) {
            printInfo();
            return;
        }

        var ctx = new SpringApplicationBuilder(AnalyzerCliApplication.class)
                .headless(false).run(args);

        ctx.getBean(AnalyzerCliApplication.class).run(args);
    }
}
