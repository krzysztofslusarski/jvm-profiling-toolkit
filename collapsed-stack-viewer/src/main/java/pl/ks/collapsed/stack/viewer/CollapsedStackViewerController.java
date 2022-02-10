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
package pl.ks.collapsed.stack.viewer;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.apache.commons.compress.utils.IOUtils;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;
import pl.ks.collapsed.stack.viewer.pages.WelcomePage;
import pl.ks.flame.graph.FlameGraphExecutor;
import pl.ks.profiling.io.StorageUtils;
import pl.ks.profiling.io.TempFileUtils;

@Controller
@RequiredArgsConstructor
class CollapsedStackViewerController {
    private final CollapsedStackPageCreator collapsedStackPageCreator;
    private final FlameGraphExecutor flameGraphExecutor;
    private final CollapsedStackComparePageCreator collapsedStackComparePageCreator;

    @GetMapping("/")
    String defualt() {
        return "upload";
    }

    @GetMapping("/upload")
    String upload() {
        return "upload";
    }

    @PostMapping("/upload")
    String upload(Model model,
                  @RequestParam("file") MultipartFile file,
                  @RequestParam("filter") String filter,
                  @RequestParam("title") String title,
                  @RequestParam("totalTimeThreshold") BigDecimal totalTimeThreshold,
                  @RequestParam("selfTimeThreshold") BigDecimal selfTimeThreshold) throws Exception {
        String originalFilename = file.getOriginalFilename();
        InputStream inputStream = StorageUtils.createCopy(TempFileUtils.TEMP_DIR, originalFilename, file.getInputStream());
        String uncompressedFileName = "collapsed-stack-" + UUID.randomUUID().toString() + ".log";
        if (StringUtils.isEmpty(filter)) {
            IOUtils.copy(inputStream, new FileOutputStream(TempFileUtils.TEMP_DIR + uncompressedFileName));
        } else {
            copyFileWithFilter(filter, inputStream, uncompressedFileName);
        }

        model.addAttribute("welcomePage", WelcomePage.builder()
                .pages(collapsedStackPageCreator.generatePages(uncompressedFileName, title, totalTimeThreshold, selfTimeThreshold))
                .title(title)
                .build());
        return "welcome";
    }

    @PostMapping("/compare")
    String compare(Model model,
                   @RequestParam("file") MultipartFile file,
                   @RequestParam("file2") MultipartFile file2,
                   @RequestParam("filter") String filter,
                   @RequestParam("totalTimeThreshold") BigDecimal totalTimeThreshold,
                   @RequestParam("selfTimeThreshold") BigDecimal selfTimeThreshold) throws Exception {
        String originalFilename1 = file.getOriginalFilename();
        String originalFilename2 = file2.getOriginalFilename();
        InputStream inputStream1 = StorageUtils.createCopy(TempFileUtils.TEMP_DIR, originalFilename1, file.getInputStream());
        InputStream inputStream2 = StorageUtils.createCopy(TempFileUtils.TEMP_DIR, originalFilename2, file2.getInputStream());
        String uncompressedFileName1 = "collapsed-stack-" + UUID.randomUUID().toString() + ".log";
        String uncompressedFileName2 = "collapsed-stack-" + UUID.randomUUID().toString() + ".log";
        if (StringUtils.isEmpty(filter)) {
            IOUtils.copy(inputStream1, new FileOutputStream(TempFileUtils.TEMP_DIR + uncompressedFileName1));
            IOUtils.copy(inputStream2, new FileOutputStream(TempFileUtils.TEMP_DIR + uncompressedFileName2));
        } else {
            copyFileWithFilter(filter, inputStream1, uncompressedFileName1);
            copyFileWithFilter(filter, inputStream2, uncompressedFileName2);
        }
        model.addAttribute("welcomePage", WelcomePage.builder()
                .pages(collapsedStackComparePageCreator.generatePages(uncompressedFileName1, uncompressedFileName2, totalTimeThreshold, selfTimeThreshold))
                .build());
        return "compare";
    }

    @GetMapping(value = "/image/{name}", produces = "text/html")
    @SneakyThrows
    @ResponseBody
    byte[] getImage(@PathVariable("name") String name) {
        return IOUtils.toByteArray(new FileInputStream(TempFileUtils.getFilePath(name)));
    }


    @GetMapping(value = "/flame-graph", produces = "text/html")
    @ResponseBody
    byte[] getFlameGraph(@RequestParam("collapsed") String collapsed,
                         @RequestParam("title") String title) throws Exception {
        String collapsedFilePath = TempFileUtils.getFilePath(collapsed);
        String outputHtmlFilePath = TempFileUtils.getFilePath(collapsed + ".html");
        flameGraphExecutor.generateFlameGraphHtml5(collapsedFilePath, outputHtmlFilePath, "Flame graph - " + title, false, false);
        byte[] response = null;
        try (InputStream inputStream = new FileInputStream(outputHtmlFilePath);) {
            response = IOUtils.toByteArray(inputStream);
        }
        Files.delete(Paths.get(outputHtmlFilePath));
        return response;
    }

    @GetMapping(value = "/flame-graph-no-thread", produces = "text/html")
    @ResponseBody
    byte[] getFlameGraphNoThread(@RequestParam("collapsed") String collapsed,
                                 @RequestParam("title") String title) throws Exception {
        String collapsedFilepath = TempFileUtils.getFilePath(collapsed);
        String outputHtmlFilePath = TempFileUtils.getFilePath(collapsed + ".no-thread.html");
        String outputCollapsedFilePath = TempFileUtils.getFilePath(collapsed + "-no-thread.txt");

        try (InputStream inputStream = new FileInputStream(collapsedFilepath);
             OutputStream outputStream = new FileOutputStream(outputCollapsedFilePath);
             BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
             BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(outputStream));) {
            while (reader.ready()) {
                String line = reader.readLine();
                writer.write(line.substring(line.indexOf(";") + 1));
                writer.newLine();
            }
        }

        flameGraphExecutor.generateFlameGraphHtml5(outputCollapsedFilePath, outputHtmlFilePath, "Flame graph - no thread division - " + title, false, false);
        byte[] response = null;
        try (InputStream inputStream = new FileInputStream(outputHtmlFilePath);) {
            response = IOUtils.toByteArray(inputStream);
        }
        Files.delete(Paths.get(outputHtmlFilePath));
        Files.delete(Paths.get(outputCollapsedFilePath));
        return response;
    }

    @GetMapping(value = "/flame-graph-hotspot-limited", produces = "text/html")
    @ResponseBody
    byte[] getFlameGraphHotspotLimited(@RequestParam("limit") int limit,
                                       @RequestParam("collapsed") String collapsed,
                                       @RequestParam("title") String title) throws Exception {
        UUID newUuid = UUID.randomUUID();
        String outputCollapsedFilePath = TempFileUtils.getFilePath(newUuid + "-method.txt");
        String outputHtmlFilePath = TempFileUtils.getFilePath(newUuid + "-method.html");
        try (InputStream inputStream = new FileInputStream(TempFileUtils.getFilePath(collapsed));
             OutputStream fromOutStream = new FileOutputStream(outputCollapsedFilePath);
             BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
             BufferedWriter toWriter = new BufferedWriter(new OutputStreamWriter(fromOutStream));
        ) {
            while (reader.ready()) {
                String line = reader.readLine();
                int delimiterChar = line.lastIndexOf(" ");
                String stack = line.substring(0, delimiterChar);
                String num = line.substring(delimiterChar + 1);

                String[] splittedStack = stack.split(";");
                for (int i = Math.max(0, splittedStack.length - limit); i < splittedStack.length; i++) {
                    String frame = splittedStack[i];
                    toWriter.write(frame);
                    if (i != splittedStack.length - 1) {
                        toWriter.write(";");
                    }
                }

                toWriter.write(" ");
                toWriter.write(num);
                toWriter.newLine();
            }
        }

        flameGraphExecutor.generateFlameGraphHtml5(outputCollapsedFilePath, outputHtmlFilePath, "Flame graph - hotspot - " + title, true, true);
        byte[] response = null;
        try (InputStream inputStream = new FileInputStream(outputHtmlFilePath);) {
            response = IOUtils.toByteArray(inputStream);
        }
        Files.delete(Paths.get(outputHtmlFilePath));
        Files.delete(Paths.get(outputCollapsedFilePath));
        return response;
    }

    @GetMapping(value = "/flame-graph-hotspot", produces = "text/html")
    @ResponseBody
    byte[] getFlameGraphHotspot(@RequestParam("collapsed") String collapsed,
                                @RequestParam("title") String title) throws Exception {
        String collapsedFileName = TempFileUtils.getFilePath(collapsed);
        String outputHtmlFilePath = TempFileUtils.getFilePath(collapsed + ".hotspot.html");
        flameGraphExecutor.generateFlameGraphHtml5(collapsedFileName, outputHtmlFilePath, "Flame graph - hotspot - " + title, true, true);
        byte[] response = null;
        try (InputStream inputStream = new FileInputStream(outputHtmlFilePath);) {
            response = IOUtils.toByteArray(inputStream);
        }
        Files.delete(Paths.get(outputHtmlFilePath));
        return response;
    }


    @GetMapping(value = "/from-method", produces = "text/html")
    @ResponseBody
    byte[] fromMethod(@RequestParam("collapsed") String collapsed, @RequestParam("method") String method,
                      @RequestParam("title") String title) throws Exception {
        UUID newUuid = UUID.randomUUID();
        String outputCollapsedFilePath = TempFileUtils.getFilePath(newUuid + "-method.txt");
        String outputHtmlFilePath = TempFileUtils.getFilePath(newUuid + "-method.html");
        try (InputStream inputStream = new FileInputStream(TempFileUtils.getFilePath(collapsed));
             OutputStream fromOutStream = new FileOutputStream(outputCollapsedFilePath);
             BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
             BufferedWriter fromWriter = new BufferedWriter(new OutputStreamWriter(fromOutStream));
        ) {
            while (reader.ready()) {
                String line = reader.readLine();
                int delimiterChar = line.lastIndexOf(" ");
                String stack = line.substring(0, delimiterChar);
                int pos = stack.indexOf(method);
                if (pos < 0) {
                    continue;
                }
                String fromStack = stack.substring(pos);
                String num = line.substring(delimiterChar + 1);
                fromWriter.write(fromStack);
                fromWriter.write(" ");
                fromWriter.write(num);
                fromWriter.newLine();
            }
        }
        flameGraphExecutor.generateFlameGraphHtml5(outputCollapsedFilePath, outputHtmlFilePath, "Callee - " + title + " - " + method, false, false);
        byte[] response = null;
        try (InputStream inputStream = new FileInputStream(outputHtmlFilePath);) {
            response = IOUtils.toByteArray(inputStream);
        }
        Files.delete(Paths.get(outputHtmlFilePath));
        Files.delete(Paths.get(outputCollapsedFilePath));
        return response;
    }

    @GetMapping(value = "/from-method-root", produces = "text/html")
    String fromMethodRoot(Model model,
                          @RequestParam("collapsed") String collapsed,
                          @RequestParam("method") String method,
                          @RequestParam("title") String title,
                          @RequestParam("totalTimeThreshold") BigDecimal totalTimeThreshold,
                          @RequestParam("selfTimeThreshold") BigDecimal selfTimeThreshold) throws Exception {
        UUID newUuid = UUID.randomUUID();
        String fileName = newUuid + "from--method-root.txt";
        String outputCollapsedFilePath = TempFileUtils.getFilePath(fileName);
        try (InputStream inputStream = new FileInputStream(TempFileUtils.getFilePath(collapsed));
             OutputStream fromOutStream = new FileOutputStream(outputCollapsedFilePath);
             BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
             BufferedWriter fromWriter = new BufferedWriter(new OutputStreamWriter(fromOutStream));
        ) {
            while (reader.ready()) {
                String line = reader.readLine();
                int delimiterChar = line.lastIndexOf(" ");
                String stack = line.substring(0, delimiterChar);
                int pos = stack.indexOf(method);
                if (pos < 0) {
                    continue;
                }
                String fromStack = stack.substring(pos);
                String num = line.substring(delimiterChar + 1);
                fromWriter.write(fromStack);
                fromWriter.write(" ");
                fromWriter.write(num);
                fromWriter.newLine();
            }
        }
        model.addAttribute("welcomePage", WelcomePage.builder()
                .pages(collapsedStackPageCreator.generatePages(fileName, title + " - " + method, totalTimeThreshold, selfTimeThreshold))
                .title(title + " " + method)
                .build());
        return "welcome";
    }


    @GetMapping(value = "/to-method", produces = "text/html")
    @ResponseBody
    byte[] toMethod(@RequestParam("collapsed") String collapsed,
                    @RequestParam("method") String method,
                    @RequestParam("title") String title) throws Exception {
        UUID newUuid = UUID.randomUUID();
        String outputCollapsedFilePath = TempFileUtils.getFilePath(newUuid + "-method.txt");
        String outputHtmlFilePath = TempFileUtils.getFilePath(newUuid + "-method.html");
        try (InputStream inputStream = new FileInputStream(TempFileUtils.getFilePath(collapsed));
             OutputStream fromOutStream = new FileOutputStream(outputCollapsedFilePath);
             BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
             BufferedWriter toWriter = new BufferedWriter(new OutputStreamWriter(fromOutStream));
        ) {
            while (reader.ready()) {
                String line = reader.readLine();
                int delimiterChar = line.lastIndexOf(" ");
                String stack = line.substring(0, delimiterChar);
                int pos = stack.indexOf(method);
                if (pos < 0) {
                    continue;
                }
                String toStack = stack.substring(0, pos + method.length());
                String num = line.substring(delimiterChar + 1);

                toWriter.write(toStack);
                toWriter.write(" ");
                toWriter.write(num);
                toWriter.newLine();
            }
        }
        flameGraphExecutor.generateFlameGraphHtml5(outputCollapsedFilePath, outputHtmlFilePath, "Callers - " + title + " - " + method, true, false);
        byte[] response = null;
        try (InputStream inputStream = new FileInputStream(outputHtmlFilePath);) {
            response = IOUtils.toByteArray(inputStream);
        }
        Files.delete(Paths.get(outputCollapsedFilePath));
        Files.delete(Paths.get(outputHtmlFilePath));
        return response;
    }

    @GetMapping(value = "/to-method-root", produces = "text/html")
    String toMethodRoot(Model model,
                        @RequestParam("collapsed") String collapsed,
                        @RequestParam("method") String method,
                        @RequestParam("title") String title,
                        @RequestParam("totalTimeThreshold") BigDecimal totalTimeThreshold,
                        @RequestParam("selfTimeThreshold") BigDecimal selfTimeThreshold) throws Exception {
        UUID newUuid = UUID.randomUUID();
        String fileName = newUuid + "-to-method-root.txt";
        String outputCollapsedFilePath = TempFileUtils.getFilePath(fileName);
        try (InputStream inputStream = new FileInputStream(TempFileUtils.getFilePath(collapsed));
             OutputStream fromOutStream = new FileOutputStream(outputCollapsedFilePath);
             BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
             BufferedWriter toWriter = new BufferedWriter(new OutputStreamWriter(fromOutStream));
        ) {
            while (reader.ready()) {
                String line = reader.readLine();
                int delimiterChar = line.lastIndexOf(" ");
                String stack = line.substring(0, delimiterChar);
                int pos = stack.indexOf(method);
                if (pos < 0) {
                    continue;
                }
                String toStack = stack.substring(0, pos + method.length());
                String num = line.substring(delimiterChar + 1);

                toWriter.write(toStack);
                toWriter.write(" ");
                toWriter.write(num);
                toWriter.newLine();
            }
        }
        model.addAttribute("welcomePage", WelcomePage.builder()
                .pages(collapsedStackPageCreator.generatePages(fileName, title + " - " + method, totalTimeThreshold, selfTimeThreshold))
                .title(title + " " + method)
                .build());
        return "welcome";
    }

    private void copyFileWithFilter(String filter, InputStream inputStream, String outputFileName) throws IOException {
        filter = filter.trim();
        try (OutputStream outputStream = new FileOutputStream(TempFileUtils.TEMP_DIR + outputFileName);
             BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
             BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(outputStream));) {
            while (reader.ready()) {
                String line = reader.readLine();
                if (line.contains(filter)) {
                    writer.write(line);
                    writer.newLine();
                }
            }
        } finally {
            inputStream.close();
        }
    }

}
