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
package pl.ks.cli;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import pl.ks.cli.state.ViewerState;
import pl.ks.cli.tui.JfrTui;
import pl.ks.viewer.StatefulJfrViewerService;

@Component
@RequiredArgsConstructor
class CliRunner implements ApplicationRunner {
    private final StatefulJfrViewerService service;

    @Override
    public void run(ApplicationArguments args) throws Exception {
        List<String> files = args.getNonOptionArgs();
        if (args.containsOption("help") || files.isEmpty()) {
            printUsage();
            return;
        }
        for (String file : files) {
            if (!Files.isRegularFile(Path.of(file))) {
                System.err.println("Not a file: " + file);
                System.exit(1);
            }
        }

        System.out.println("Parsing " + files.size() + " file(s)...");
        Instant start = Instant.now();
        UUID fileId = service.parseNewFiles(files,
                flag(args, "old-async-profiler"),
                flag(args, "wall-clock-exact-time"),
                flag(args, "unify-lambdas"),
                flag(args, "throw-on-errored-file"),
                flag(args, "cross-file-span-matching"));
        System.out.println("Parsed in " + Duration.between(start, Instant.now()).toMillis() + " ms");

        ViewerState state = new ViewerState();
        state.setTableLimit(intOption(args, "table-limit", state.getTableLimit()));

        try {
            new JfrTui(service, fileId, state).run();
        } catch (IllegalArgumentException e) {
            System.err.println(e.getMessage());
            System.exit(1);
        }
    }

    private static boolean flag(ApplicationArguments args, String name) {
        if (!args.containsOption(name)) {
            return false;
        }
        List<String> values = args.getOptionValues(name);
        return values == null || values.isEmpty() || Boolean.parseBoolean(values.get(0));
    }

    private static int intOption(ApplicationArguments args, String name, int defaultValue) {
        List<String> values = args.getOptionValues(name);
        return values == null || values.isEmpty() ? defaultValue : Integer.parseInt(values.get(0));
    }

    private static void printUsage() {
        System.out.println("""
                Console viewer for JFR files - flame graph, total time, self time and span stats.

                Usage:
                  java -jar jfr-cli.jar [options] <file.jfr> [<file.jfr> ...]

                Parsing options:
                  --old-async-profiler        files were recorded with async-profiler older than 2.9
                  --wall-clock-exact-time     use the exact time of wall-clock samples
                  --unify-lambdas             merge lambda classes differing only by their generated suffix
                  --cross-file-span-matching  match spans with events recorded in the other files
                  --throw-on-errored-file     fail instead of skipping a file that cannot be parsed

                View options:
                  --table-limit=<rows>        maximum number of rows in the tables (default 10000)

                Every filter and option is available once started - press ? for the shortcuts.""");
    }
}
