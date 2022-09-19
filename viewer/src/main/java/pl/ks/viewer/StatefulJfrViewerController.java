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

import lombok.RequiredArgsConstructor;
import org.apache.commons.compress.utils.IOUtils;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;
import pl.ks.jfr.parser.JfrParsedFile;
import pl.ks.viewer.io.TempFileUtils;

import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static pl.ks.viewer.JfrControllerCommon.createConfig;

@Controller
@RequiredArgsConstructor
class StatefulJfrViewerController {
    public static final String ON = "on";
    private final StatefulJfrViewerService jfrViewerService;

    @GetMapping("/upload-stateful-jfr")
    String uploadJfr(Model model) {
        model.addAttribute("files", jfrViewerService.getFiles());
        return "upload-stateful-jfr";
    }

    @PostMapping("/upload-stateful-jfr")
    String upload(Model model, @RequestParam("files") MultipartFile[] files) throws Exception {
        List<String> savedCopies = new ArrayList<>(files.length);
        for (MultipartFile file : files) {
            String originalFilename = file.getOriginalFilename();
            String filePath = TempFileUtils.TEMP_DIR + originalFilename;
            IOUtils.copy(file.getInputStream(), new FileOutputStream(filePath));
            savedCopies.add(filePath);
        }
        jfrViewerService.parseNewFiles(savedCopies);
        return uploadJfr(model);
    }

    @GetMapping("/stateful-jfr/single")
    String showJfr(Model model, @RequestParam("id") UUID uuid) {
        JfrParsedFile file = jfrViewerService.getFile(uuid);
        model.addAttribute("file", file);
        model.addAttribute("currentId", uuid);
        return "uploaded-stateful-jfr";
    }

    @GetMapping("/stateful-jfr/single/remove")
    String removeJfr(Model model, @RequestParam("id") UUID uuid) {
        jfrViewerService.remove(uuid);
        return uploadJfr(model);
    }

    @ResponseBody
    @GetMapping("/stateful-jfr/single/samples/execution")
    byte[] getExecutionSamples(@RequestParam("id") UUID uuid, @RequestParam Map<String, String> params) {
        return jfrViewerService.getExecutionSamples(uuid, createConfig(params));
    }

    @ResponseBody
    @GetMapping("/stateful-jfr/single/samples/allocation/count")
    byte[] getAllocationSamplesCount(@RequestParam("id") UUID uuid, @RequestParam Map<String, String> params) {
        return jfrViewerService.getAllocationSamplesCount(uuid, createConfig(params));
    }

    @ResponseBody
    @GetMapping("/stateful-jfr/single/samples/allocation/size")
    byte[] getAllocationSamplesSize(@RequestParam("id") UUID uuid, @RequestParam Map<String, String> params) {
        return jfrViewerService.getAllocationSamplesSize(uuid, createConfig(params));
    }

    @ResponseBody
    @GetMapping("/stateful-jfr/single/samples/lock")
    byte[] getLockSamples(@RequestParam("id") UUID uuid, @RequestParam Map<String, String> params) {
        return jfrViewerService.getLockSamples(uuid, createConfig(params));
    }

    @GetMapping("/stateful-jfr/single/correlation-id-stats")
    String getCorrelationIdStats(Model model, @RequestParam("id") UUID uuid, @RequestParam Map<String, String> params) {
        model.addAttribute("correlationIdStats", jfrViewerService.getCorrelationIdStats(uuid, createConfig(params)));
        return "uploaded-stateful-jfr-correlation-id-stats";
    }

    @GetMapping("/stateful-jfr/single/cpu-stats")
    String getCpuStats(Model model, @RequestParam("id") UUID uuid, @RequestParam Map<String, String> params) {
        model.addAttribute("cpuStats", jfrViewerService.getCpuStats(uuid, createConfig(params)));
        return "uploaded-stateful-jfr-cpu-stats";
    }
}
