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
package pl.ks.viewer;

import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.apache.commons.compress.utils.IOUtils;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import pl.ks.jfr.parser.JfrParsedFile;
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
            boolean isGZip = originalFilename != null && originalFilename.endsWith(".gz");
            String fileName = "jfr-" + UUID.randomUUID().toString() + ".jfr" +
                    (isGZip ? ".gz" : "");
            String filePath = TempFileUtils.TEMP_DIR + fileName;
            IOUtils.copy(file.getInputStream(), new FileOutputStream(filePath));
            savedCopies.add(filePath);
        }
        Map<JfrParsedFile.Type, String> converted = jfrViewerService.convertToCollapsed(savedCopies);
        model.addAttribute("collapsed", converted.entrySet());
        return "uploaded-jfr";
    }
}
