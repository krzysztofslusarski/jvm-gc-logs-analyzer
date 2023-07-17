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
package pl.ks.profiling.safepoint.analyzer.web;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.unit.DataSize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import pl.ks.profiling.io.InputUtils;
import pl.ks.profiling.io.StorageUtils;
import pl.ks.profiling.safepoint.analyzer.commons.shared.ParserUtils;
import pl.ks.profiling.safepoint.analyzer.commons.shared.report.JvmLogFile;
import pl.ks.profiling.web.commons.WelcomePage;

import jakarta.servlet.http.HttpServletRequest;

import static org.springframework.http.HttpStatus.NOT_FOUND;

@Slf4j
@Controller
@RequiredArgsConstructor
class StatsController {
    @Value("${safepoint.files.dir}")
    private String INPUTS_PATH;

    @Value("${spring.servlet.multipart.max-file-size}")
    private DataSize maxFileSize;

    @Value("${indexPageAvailable}")
    private boolean indexPageAvailable;

    @Value("${dockerImage}")
    private String dockerImage;

    private final ParsingProperties parsingProperties;
    private final StatsRepository statsRepository;
    private final ParsingExecutor parsingExecutor;

    @GetMapping("/")
    String indexDefault(Model model, HttpServletRequest request) {
        if (indexPageAvailable) {
            model.addAttribute("dockerImage", dockerImage);
            return "index";
        } else {
            return upload(model, request);
        }
    }

    @GetMapping("/upload")
    String upload(Model model, HttpServletRequest request) {
        String enqueueUrl = serverUrl(request) + "/enqueue";
        model.addAttribute("enqueueUrl", enqueueUrl);
        model.addAttribute("parsingProperties", parsingProperties);
        model.addAttribute("maxFileSize", maxFileSize);
        return "upload";
    }

    @PostMapping("/enqueue")
    @ResponseBody
    ParsingStatus enqueue(@RequestParam("file") MultipartFile file, HttpServletRequest request) throws Exception {
        String originalFilename = file.getOriginalFilename();
        log.info("New request to enqueue file {}. Copying to persistent storage", originalFilename);
        log.debug("Copying file {} to persistent storage.", originalFilename);
        String savedLocation = StorageUtils.createCopy(INPUTS_PATH, originalFilename, file.getInputStream());
        log.debug("File {} has been copied. Enqueuing.", originalFilename);
        ParsingStatus initialStatus = parsingExecutor.enqueue(
                InputUtils.getLogsSource(savedLocation, originalFilename, ParserUtils::getTimeStamp),
                (String parsingId) -> createParsingProgressUrl(request, parsingId));
        log.debug("File {} has received status {}", originalFilename, initialStatus);
        return initialStatus;
    }

    @PostMapping("/enqueue-plain-text")
    @ResponseBody
    ParsingStatus enqueue(String text, HttpServletRequest request) throws Exception {
        log.info("New request to enqueue logs of length {} characters.", text.length());
        log.debug("Saving text to persistent storage");
        String savedLocation = StorageUtils.savePlainText(INPUTS_PATH, text);
        log.debug("Enqueuing logs for parsing.");
        ParsingStatus initialStatus = parsingExecutor.enqueue(
                InputUtils.getLogsSource(savedLocation, "plain-text.log", ParserUtils::getTimeStamp),
                (String parsingId) -> createParsingProgressUrl(request, parsingId));
        log.debug("Logs have received status {}", initialStatus);
        return initialStatus;
    }

    @GetMapping(value = "/parsings/{parsingId}/progress")
    String progress(Model model, @PathVariable String parsingId) {
        model.addAttribute("initialStatus", parsingExecutor.getParsingStatus(parsingId));
        return "waiting-for-results";
    }


    @GetMapping(value = "/parsings/{parsingId}/status")
    @ResponseBody
    ParsingStatus status(@PathVariable String parsingId) {
        log.debug("Getting status of parsing {}", parsingId);
        return parsingExecutor.getParsingStatus(parsingId);
    }

    @GetMapping("/parsings/{parsingId}")
    String getParsing(Model model, @PathVariable String parsingId) {
        log.debug("Getting parsing {}", parsingId);
        JvmLogFile stats = statsRepository.get(parsingId);
        if (stats == null) {
            throw new ResponseStatusException(NOT_FOUND, "Unable to find parsing " + parsingId);
        }
        model.addAttribute("welcomePage",
                WelcomePage.builder()
                        .pages(stats.getPages())
                        .build());
        return "welcome";
    }

    private String createParsingProgressUrl(HttpServletRequest request, String parsingId) {
        return serverUrl(request) + "/parsings/" + parsingId + "/progress";
    }

    private static String serverUrl(HttpServletRequest request) {
        return request.getScheme() + "://" + request.getServerName() + (request.getServerPort() != 80 ? ":" + request.getServerPort() : "");
    }
}
