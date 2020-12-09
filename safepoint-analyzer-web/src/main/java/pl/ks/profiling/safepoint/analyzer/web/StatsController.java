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
package pl.ks.profiling.safepoint.analyzer.web;

import java.io.InputStream;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import pl.ks.profiling.io.StorageUtils;
import pl.ks.profiling.safepoint.analyzer.commons.shared.JvmLogFile;
import pl.ks.profiling.safepoint.analyzer.commons.shared.StatsService;
import pl.ks.profiling.web.commons.WelcomePage;

@Controller
@RequiredArgsConstructor
class StatsController {
    @Value("${safepoint.files.dir}")
    private String INPUTS_PATH;

    private final StatsService statsService;

    @GetMapping("/")
    String indexDefault() {
        return "index";
    }

    @GetMapping("/upload")
    String upload() {
        return "upload";
    }

    @PostMapping("/upload")
    String upload(Model model, @RequestParam("file") MultipartFile file) throws Exception {
        String originalFilename = file.getOriginalFilename();
        InputStream inputStream = StorageUtils.createCopy(INPUTS_PATH, originalFilename, file.getInputStream());
        JvmLogFile stats = statsService.createAllStats(inputStream, originalFilename);
        model.addAttribute("welcomePage", WelcomePage.builder()
                .pages(stats.getPages())
                .build());
        return "welcome";
    }

    @PostMapping("/upload-plain-text")
    String upload(Model model, String text) throws Exception {
        InputStream inputStream = StorageUtils.savePlainText(INPUTS_PATH, text);
        JvmLogFile stats = statsService.createAllStats(inputStream, "plain-text.log");
        model.addAttribute("welcomePage", WelcomePage.builder()
                .pages(stats.getPages())
                .build());
        return "welcome";
    }
}
