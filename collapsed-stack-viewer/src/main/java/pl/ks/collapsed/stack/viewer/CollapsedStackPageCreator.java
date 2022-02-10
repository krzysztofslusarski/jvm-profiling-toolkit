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
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import pl.ks.collapsed.stack.viewer.creator.FlameGraphsCreator;
import pl.ks.collapsed.stack.viewer.creator.SelfTimeCreator;
import pl.ks.collapsed.stack.viewer.creator.TotalTimeCreator;
import pl.ks.collapsed.stack.viewer.pages.Page;
import pl.ks.collapsed.stack.viewer.pages.PageCreator;
import pl.ks.profiling.io.TempFileUtils;

@Slf4j
@RequiredArgsConstructor
class CollapsedStackPageCreator {
    List<Page> generatePages(String collapsedStackFile,
                             String title,
                             BigDecimal totalTimeThreshold,
                             BigDecimal selfTimeThreshold) {
        CollapsedStackInfo collapsedStackInfo = parse(collapsedStackFile);

        List<PageCreator> pageCreators = List.of(
                new FlameGraphsCreator(collapsedStackFile, "Flame graphs", title),
                new TotalTimeCreator(collapsedStackInfo, collapsedStackFile, totalTimeThreshold, selfTimeThreshold, title),
                new SelfTimeCreator(collapsedStackInfo, collapsedStackFile, totalTimeThreshold, selfTimeThreshold, title)
        );

        return pageCreators.stream()
                .map(PageCreator::create)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    private CollapsedStackInfo parse(String fileName) {
        Map<String, MethodInfo> methodInfos = new HashMap<>();
        long totalCount = 0;

        try (InputStream inputStream = new FileInputStream(TempFileUtils.getFilePath(fileName));
             BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
             ) {
            while (reader.ready()) {
                String line = reader.readLine();
                int delimiterChar = line.lastIndexOf(" ");
                String stack = line.substring(0, delimiterChar);
                String[] splittedStack = stack.split(";");
                String num = line.substring(delimiterChar + 1);
                long currentCount = Long.parseLong(num);
                totalCount += currentCount;

                Set<String> processed = new HashSet<>();
                for (int i = 0; i < splittedStack.length; i++) {
                    String methodOnStack = splittedStack[i];
                    MethodInfo methodInfo = methodInfos.computeIfAbsent(methodOnStack, name -> MethodInfo.builder().name(name).build());
                    if (processed.add(methodOnStack)) {
                        methodInfo.addTotalSamples(currentCount);
                    }
                    if (i == splittedStack.length - 1) {
                        methodInfo.addSelfSamples(currentCount);
                    }
                }
            }
        } catch (Exception e) {
            log.error("Cannot create no thread collapsed stack", e);
            return null;
        }
        return CollapsedStackInfo.builder()
                .methods(methodInfos.values())
                .totalCount(totalCount)
                .build();
    }
}
