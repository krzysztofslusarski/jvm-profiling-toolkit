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
package pl.ks.flame.graph;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;

@RequiredArgsConstructor
public class FlameGraphExecutor {
    @SneakyThrows
    public void generateFlameGraphHtml5(String inputFile, String outputFile, String title, boolean reversed, boolean hotspot) {
        String[] args;
        if (reversed) {
            args = new String[]{"--title" , title, "--reverse", inputFile, outputFile};
        } else {
            args = new String[]{"--title" , title, inputFile, outputFile};
        }
        FlameGraph flameGraph = new FlameGraph(args);
        flameGraph.parse();
        flameGraph.dump();
    }
}
