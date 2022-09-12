/*
 * Copyright 2022 Krzysztof Slusarski
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
package pl.ks.viewer;

import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.apache.commons.compress.utils.IOUtils;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import pl.ks.jfr.parser.tuning.AdditionalLevel;
import pl.ks.viewer.io.TempFileUtils;

@Controller
@RequiredArgsConstructor
class JfrViewerController {
    public static final String ON = "on";
    private final JfrViewerService jfrViewerService;

    @GetMapping("/upload-jfr")
    String uploadJfr() {
        return "upload-jfr";
    }

    @PostMapping("/upload-jfr")
    String upload(Model model,
                  @RequestParam Map<String, String> params,
                  @RequestParam("files") MultipartFile[] files) throws Exception {
        List<String> savedCopies = new ArrayList<>(files.length);
        for (MultipartFile file : files) {
            String originalFilename = file.getOriginalFilename();
            String filePath = TempFileUtils.TEMP_DIR + originalFilename;
            IOUtils.copy(file.getInputStream(), new FileOutputStream(filePath));
            savedCopies.add(filePath);
        }
        JfrViewerResult converted = jfrViewerService.convertToCollapsed(savedCopies, createConfig(params));
        model.addAttribute("collapsed", converted.getCollapsedFiles().entrySet());
        model.addAttribute("jfr", converted.getJfrCollapsedParsedFile());
        return "uploaded-jfr";
    }

    private JfrViewerFilterAndLevelConfig createConfig(Map<String, String> params) {
        boolean threadFilterOn = ON.equals(params.get("threadFilterOn"));
        JfrViewerFilterAndLevelConfig.JfrViewerFilterAndLevelConfigBuilder builder = JfrViewerFilterAndLevelConfig.builder();
        builder.threadFilterOn(threadFilterOn);
        if (threadFilterOn) {
            builder.threadFilter(params.get("threadFilter"));
        }

        boolean endDurationOn = ON.equals(params.get("endDurationOn"));
        builder.endDurationOn(endDurationOn);
        if (endDurationOn) {
            builder
                    .duration(Long.parseLong(params.get("duration")))
                    .endDate(params.get("endDate"))
                    .endDateDateTimeFormat(params.get("endDateDateTimeFormat"));
        }

        boolean warmupCooldownOn = ON.equals(params.get("warmupCooldownOn"));
        builder.warmupCooldownOn(warmupCooldownOn);
        if (warmupCooldownOn) {
            builder
                    .cooldown(Integer.parseInt(params.get("cooldown")))
                    .warmup(Integer.parseInt(params.get("warmup")));
        }

        boolean warmupDurationOn = ON.equals(params.get("warmupDurationOn"));
        builder.warmupDurationOn(warmupDurationOn);
        if (warmupDurationOn) {
            builder
                    .wdDuration(Long.parseLong(params.get("wdDuration")))
                    .wdWarmup(Integer.parseInt(params.get("wdWarmup")));
        }

        boolean ecidFilterOn = ON.equals(params.get("ecidFilterOn"));
        builder.ecidFilterOn(ecidFilterOn);
        if (ecidFilterOn) {
            builder.ecidFilter(Long.parseLong(params.get("ecidFilter")));
        }

        boolean startEndTimestampOn = ON.equals(params.get("startEndTimestampOn"));
        builder.startEndTimestampOn(startEndTimestampOn);
        if (startEndTimestampOn) {
            builder.startTs(Long.parseLong(params.get("startTs")));
            builder.endTs(Long.parseLong(params.get("endTs")));
        }

        Set<AdditionalLevel> additionalLevels = new HashSet<>();
        if (ON.equals(params.get("extractThreads"))) {
            additionalLevels.add(AdditionalLevel.THREAD);
        }
        if (ON.equals(params.get("extractTs10S"))) {
            additionalLevels.add(AdditionalLevel.TIMESTAMP_10_S);
        }
        if (ON.equals(params.get("extractTs1S"))) {
            additionalLevels.add(AdditionalLevel.TIMESTAMP_1_S);
        }
        if (ON.equals(params.get("extractTs100Ms"))) {
            additionalLevels.add(AdditionalLevel.TIMESTAMP_100_MS);
        }
        if (ON.equals(params.get("extractFilename"))) {
            additionalLevels.add(AdditionalLevel.FILENAME);
        }
        if (ON.equals(params.get("extractEcid"))) {
            additionalLevels.add(AdditionalLevel.ECID);
        }
        builder.additionalLevels(additionalLevels);

        return builder.build();
    }
}
