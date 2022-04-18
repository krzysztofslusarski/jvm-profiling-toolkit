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
package pl.ks.collapsed;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import java.util.zip.GZIPOutputStream;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class CollapsedStackWriter {
    public static void saveFile(String dir, String filename, CollapsedStack collapsedStack) throws IOException {
        log.info("Writing to dir: {} with file name: {}", dir, filename);
        try (Writer output = new OutputStreamWriter(new FileOutputStream(dir + "/" + filename ))) {
            for (Map.Entry<String, AtomicLong> holderEntry : collapsedStack.stacks().entrySet()) {
                write(output, holderEntry);
            }
        }
    }

    public static void saveGZipFile(String dir, String filename, CollapsedStack collapsedStack) throws IOException {
        log.info("Writing to dir: {} with file name: {}.gz", dir, filename);
        try (Writer output = new OutputStreamWriter(new GZIPOutputStream(new FileOutputStream(dir + "/" + filename + ".gz")))) {
            for (Map.Entry<String, AtomicLong> holderEntry : collapsedStack.stacks().entrySet()) {
                write(output, holderEntry);
            }
        }
    }

    private static void write(Writer output, Map.Entry<String, AtomicLong> holderEntry) throws IOException {
        output.write(holderEntry.getKey());
        output.write(" ");
        output.write("" + holderEntry.getValue().get());
        output.write("\n");
    }
}
