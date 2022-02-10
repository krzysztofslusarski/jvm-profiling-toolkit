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
import pl.ks.collapsed.stack.viewer.creator.SelfTimeCompareCreator;
import pl.ks.collapsed.stack.viewer.creator.TotalTimeCompareCreator;
import pl.ks.collapsed.stack.viewer.pages.Page;
import pl.ks.collapsed.stack.viewer.pages.PageCreator;
import pl.ks.profiling.io.TempFileUtils;

@Slf4j
@RequiredArgsConstructor
class CollapsedStackComparePageCreator {
    List<Page> generatePages(String collapsedStackFile1, String collapsedStackFile2, BigDecimal totalTimeThreshold, BigDecimal selfTimeThreshold) {
        CollapsedStackCompareInfo collapsedStackInfo = parse(collapsedStackFile1, collapsedStackFile2);

        List<PageCreator> pageCreators = List.of(
                new FlameGraphsCreator(collapsedStackFile1, "Flame graphs - file 1", "File 1"),
                new FlameGraphsCreator(collapsedStackFile2, "Flame graphs - file 2", "File 2"),
                new TotalTimeCompareCreator(collapsedStackInfo, collapsedStackFile1, collapsedStackFile2, totalTimeThreshold, "Compare"),
                new SelfTimeCompareCreator(collapsedStackInfo, collapsedStackFile1, collapsedStackFile2, selfTimeThreshold, "Compare")
        );

        return pageCreators.stream()
                .map(PageCreator::create)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    private CollapsedStackCompareInfo parse(String fileName1, String fileName2) {
        Map<String, MethodCompareInfo> methodInfos = new HashMap<>();
        long totalCount1 = 0;
        long totalCount2 = 0;

        try (InputStream inputStream = new FileInputStream(TempFileUtils.getFilePath(fileName1));
             BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
        ) {
            while (reader.ready()) {
                String line = reader.readLine();
                int delimiterChar = line.lastIndexOf(" ");
                String stack = line.substring(0, delimiterChar);
                String[] splittedStack = stack.split(";");
                String num = line.substring(delimiterChar + 1);
                long currentCount = Long.parseLong(num);
                totalCount1 += currentCount;

                Set<String> processed = new HashSet<>();
                for (int i = 0; i < splittedStack.length; i++) {
                    String methodOnStack = splittedStack[i];
                    MethodCompareInfo methodInfo = methodInfos.computeIfAbsent(methodOnStack, name -> MethodCompareInfo.builder().name(name).build());
                    if (processed.add(methodOnStack)) {
                        methodInfo.addTotalSamples1(currentCount);
                    }
                    if (i == splittedStack.length - 1) {
                        methodInfo.addSelfSamples1(currentCount);
                    }
                }
            }
        } catch (Exception e) {
            log.error("Cannot create no thread collapsed stack", e);
            return null;
        }

        try (InputStream inputStream = new FileInputStream(TempFileUtils.getFilePath(fileName2));
             BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
        ) {
            while (reader.ready()) {
                String line = reader.readLine();
                int delimiterChar = line.lastIndexOf(" ");
                String stack = line.substring(0, delimiterChar);
                String[] splittedStack = stack.split(";");
                String num = line.substring(delimiterChar + 1);
                long currentCount = Long.parseLong(num);
                totalCount2 += currentCount;

                Set<String> processed = new HashSet<>();
                for (int i = 0; i < splittedStack.length; i++) {
                    String methodOnStack = splittedStack[i];
                    MethodCompareInfo methodInfo = methodInfos.computeIfAbsent(methodOnStack, name -> MethodCompareInfo.builder().name(name).build());
                    if (processed.add(methodOnStack)) {
                        methodInfo.addTotalSamples2(currentCount);
                    }
                    if (i == splittedStack.length - 1) {
                        methodInfo.addSelfSamples2(currentCount);
                    }
                }
            }
        } catch (Exception e) {
            log.error("Cannot create no thread collapsed stack", e);
            return null;
        }

        return CollapsedStackCompareInfo.builder()
                .methods(methodInfos.values())
                .totalCount1(totalCount1)
                .totalCount2(totalCount2)
                .build();
    }
}
