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

import static pl.ks.viewer.JfrControllerCommon.createConfig;

import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.apache.commons.compress.utils.IOUtils;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import pl.ks.viewer.io.TempFileUtils;

@Controller
@RequiredArgsConstructor
class JfrViewerController {
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

}
