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
import static pl.ks.viewer.TimeTable.Type.SELF_TIME;
import static pl.ks.viewer.TimeTable.Type.TOTAL_TIME;

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

    @GetMapping("/stateful-jfr/single/trim")
    String trimToMethod(Model model, @RequestParam("id") UUID uuid,
                        @RequestParam("methodName") String methodName,
                        @RequestParam("direction") JfrParsedFile.Direction direction) {
        jfrViewerService.trimToMethod(uuid, methodName, direction);
        return uploadJfr(model);
    }

    @ResponseBody
    @GetMapping("/stateful-jfr/single/flames/execution")
    byte[] getExecutionSamplesFlameGraph(@RequestParam("id") UUID uuid, @RequestParam Map<String, String> params) {
        return jfrViewerService.getExecutionSamplesFlameGraph(uuid, createConfig(params));
    }

    @ResponseBody
    @GetMapping("/stateful-jfr/single/flames/allocation/count")
    byte[] getAllocationSamplesCountFlameGraph(@RequestParam("id") UUID uuid, @RequestParam Map<String, String> params) {
        return jfrViewerService.getAllocationCountSamplesFlameGraph(uuid, createConfig(params));
    }

    @ResponseBody
    @GetMapping("/stateful-jfr/single/flames/allocation/size")
    byte[] getAllocationSamplesSizeFlameGraph(@RequestParam("id") UUID uuid, @RequestParam Map<String, String> params) {
        return jfrViewerService.getAllocationSizeSamplesFlameGraph(uuid, createConfig(params));
    }

    @ResponseBody
    @GetMapping("/stateful-jfr/single/flames/lock/count")
    byte[] getLockCountSamplesFlameGraph(@RequestParam("id") UUID uuid, @RequestParam Map<String, String> params) {
        return jfrViewerService.getLockCountSamplesFlameGraph(uuid, createConfig(params));
    }

    @ResponseBody
    @GetMapping("/stateful-jfr/single/flames/lock/time")
    byte[] getLockTimeSamplesFlameGraph(@RequestParam("id") UUID uuid, @RequestParam Map<String, String> params) {
        return jfrViewerService.getLockTimeSamplesFlameGraph(uuid, createConfig(params));
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

    @GetMapping("/stateful-jfr/single/table/total/execution")
    String getExecutionTotalTimeTable(Model model, @RequestParam("id") UUID uuid, @RequestParam Map<String, String> params) {
        model.addAttribute("table", jfrViewerService.getExecutionSamplesTimeStats(uuid, createConfig(params), TOTAL_TIME));
        return "uploaded-stateful-total-time-table";
    }

    @GetMapping("/stateful-jfr/single/table/total/allocation/count")
    String getAllocationCountTotalTimeStats(Model model, @RequestParam("id") UUID uuid, @RequestParam Map<String, String> params) {
        model.addAttribute("table", jfrViewerService.getAllocationCountSamplesTimeStats(uuid, createConfig(params), TOTAL_TIME));
        return "uploaded-stateful-total-time-table";
    }

    @GetMapping("/stateful-jfr/single/table/total/allocation/size")
    String getAllocationSizeTotalTimeStats(Model model, @RequestParam("id") UUID uuid, @RequestParam Map<String, String> params) {
        model.addAttribute("table", jfrViewerService.getAllocationSizeSamplesTimeStats(uuid, createConfig(params), TOTAL_TIME));
        return "uploaded-stateful-total-time-table";
    }

    @GetMapping("/stateful-jfr/single/table/total/lock/count")
    String getLockCountTotalTimeStats(Model model, @RequestParam("id") UUID uuid, @RequestParam Map<String, String> params) {
        model.addAttribute("table", jfrViewerService.getLockCountSamplesTimeStats(uuid, createConfig(params), TOTAL_TIME));
        return "uploaded-stateful-total-time-table";
    }

    @GetMapping("/stateful-jfr/single/table/total/lock/time")
    String getLockTimeTotalTimeStats(Model model, @RequestParam("id") UUID uuid, @RequestParam Map<String, String> params) {
        model.addAttribute("table", jfrViewerService.getLockTimeSamplesTimeStats(uuid, createConfig(params), TOTAL_TIME));
        return "uploaded-stateful-total-time-table";
    }

    @GetMapping("/stateful-jfr/single/table/self/execution")
    String getExecutionSelfTimeTable(Model model, @RequestParam("id") UUID uuid, @RequestParam Map<String, String> params) {
        model.addAttribute("table", jfrViewerService.getExecutionSamplesTimeStats(uuid, createConfig(params), SELF_TIME));
        return "uploaded-stateful-self-time-table";
    }

    @GetMapping("/stateful-jfr/single/table/self/allocation/count")
    String getAllocationCountSelfTimeStats(Model model, @RequestParam("id") UUID uuid, @RequestParam Map<String, String> params) {
        model.addAttribute("table", jfrViewerService.getAllocationCountSamplesTimeStats(uuid, createConfig(params), SELF_TIME));
        return "uploaded-stateful-self-time-table";
    }

    @GetMapping("/stateful-jfr/single/table/self/allocation/size")
    String getAllocationSizeSelfTimeStats(Model model, @RequestParam("id") UUID uuid, @RequestParam Map<String, String> params) {
        model.addAttribute("table", jfrViewerService.getAllocationSizeSamplesTimeStats(uuid, createConfig(params), SELF_TIME));
        return "uploaded-stateful-self-time-table";
    }

    @GetMapping("/stateful-jfr/single/table/self/lock/count")
    String getLockCountSelfTimeStats(Model model, @RequestParam("id") UUID uuid, @RequestParam Map<String, String> params) {
        model.addAttribute("table", jfrViewerService.getLockCountSamplesTimeStats(uuid, createConfig(params), SELF_TIME));
        return "uploaded-stateful-self-time-table";
    }

    @GetMapping("/stateful-jfr/single/table/self/lock/time")
    String getLockTimeSelfTimeStats(Model model, @RequestParam("id") UUID uuid, @RequestParam Map<String, String> params) {
        model.addAttribute("table", jfrViewerService.getLockTimeSamplesTimeStats(uuid, createConfig(params), SELF_TIME));
        return "uploaded-stateful-self-time-table";
    }
}
