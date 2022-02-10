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
package pl.ks.collapsed.stack.viewer.creator;

import static pl.ks.collapsed.stack.viewer.pages.TableWithLinks.Link.of;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import pl.ks.collapsed.stack.viewer.CollapsedStackCompareInfo;
import pl.ks.collapsed.stack.viewer.MethodCompareInfo;
import pl.ks.collapsed.stack.viewer.pages.Page;
import pl.ks.collapsed.stack.viewer.pages.PageCreator;
import pl.ks.collapsed.stack.viewer.pages.PageCreatorHelper;
import pl.ks.collapsed.stack.viewer.pages.TableWithLinks;

@RequiredArgsConstructor
public class SelfTimeCompareCreator implements PageCreator {
    private final CollapsedStackCompareInfo collapsedStackCompareInfo;
    private final String collapsedStackFileName1;
    private final String collapsedStackFileName2;
    private final BigDecimal threshold;
    private final String title;

    @Override
    public Page create() {
        DecimalFormat decimalFormat = new DecimalFormat("#,##0.00", DecimalFormatSymbols.getInstance(Locale.US));
        BigDecimal totalCount1 = new BigDecimal(collapsedStackCompareInfo.getTotalCount1());
        BigDecimal totalCount2 = new BigDecimal(collapsedStackCompareInfo.getTotalCount2());

        List<MethodCompareInfo> methodsToList = collapsedStackCompareInfo.getMethods().stream()
                .filter(methodEntry -> overThreshold(totalCount1, methodEntry))
                .collect(Collectors.toList());
        methodsToList.forEach(methodCompareInfo -> methodCompareInfo.calculatePercents(totalCount1, totalCount2));
        methodsToList.sort(Comparator.comparing(MethodCompareInfo::getSelfDiff).reversed());

        return Page.builder()
                .fullName("Method self time")
                .menuName("Method self time")
                .icon(Page.Icon.STATS)
                .pageContents(List.of(
                        TableWithLinks.builder()
                                .title("Methods not found in the 1st file")
                                .header(getHeaderWithoutFirstFile())
                                .filteredColumn(0)
                                .table(createWithoutFirstFile(decimalFormat, methodsToList, methodCompareInfo -> methodCompareInfo.getTotalSamples1() == 0, Comparator.comparing(MethodCompareInfo::getSelfDiff).reversed()))
                                .build(),
                        TableWithLinks.builder()
                                .title("Methods not found in the 2nd file")
                                .header(getHeaderWithoutSecondFile())
                                .filteredColumn(0)
                                .table(createWithoutSecondFile(decimalFormat, methodsToList, methodCompareInfo -> methodCompareInfo.getTotalSamples2() == 0, Comparator.comparing(MethodCompareInfo::getSelfDiff)))
                                .build(),
                        TableWithLinks.builder()
                                .title("Methods that used more resources in the 1st file then in the 2nd file")
                                .header(getHeader())
                                .filteredColumn(0)
                                .table(create(decimalFormat, methodsToList, methodCompareInfo -> methodCompareInfo.getTotalSamples2() != 0 && methodCompareInfo.getSelfDiff().compareTo(BigDecimal.ZERO) < 0, Comparator.comparing(MethodCompareInfo::getSelfDiff)))
                                .build(),
                        TableWithLinks.builder()
                                .title("Methods that used more resources in the 2nd file then in the 1st file")
                                .header(getHeader())
                                .filteredColumn(0)
                                .table(create(decimalFormat, methodsToList, methodCompareInfo -> methodCompareInfo.getTotalSamples1() != 0 && methodCompareInfo.getSelfDiff().compareTo(BigDecimal.ZERO) > 0, Comparator.comparing(MethodCompareInfo::getSelfDiff).reversed()))
                                .build(),
                        TableWithLinks.builder()
                                .title("Methods that used the same amount of resources")
                                .header(getHeader())
                                .filteredColumn(0)
                                .table(create(decimalFormat, methodsToList, methodCompareInfo -> methodCompareInfo.getTotalSamples1() != 0 && methodCompareInfo.getSelfDiff().compareTo(BigDecimal.ZERO) == 0, Comparator.comparing(MethodCompareInfo::getSelfDiff).reversed()))
                                .build()
                        ))
                .build();
    }

    private List<String> getHeader() {
        return List.of("Method", "Self time % diff (2-1)", "Self time % (1 -> 2)", "Callee FG 1", "Callers FG 1", "Callee FG 2", "Callers FG 2");
    }

    private List<List<TableWithLinks.Link>> create(DecimalFormat decimalFormat, List<MethodCompareInfo> methodsToList, Predicate<? super MethodCompareInfo> filter, Comparator<MethodCompareInfo> comparator) {
        return methodsToList.stream()
                .filter(filter)
                .sorted(comparator)
                .map(methodEntry -> List.of(
                        of(methodEntry.getName()),
                        of(getPercent(methodEntry.getSelfDiff(), decimalFormat)),
                        of(getPercent(methodEntry.getSelfPercent1(), decimalFormat) + " --> " + getPercent(methodEntry.getSelfPercent2(), decimalFormat)),
                        of(LinkUtils.getFromMethodHref(collapsedStackFileName1, title, methodEntry.getName()), "Callee"),
                        of(LinkUtils.getToMethodHref(collapsedStackFileName1, title, methodEntry.getName()), "Callers"),
                        of(LinkUtils.getFromMethodHref(collapsedStackFileName2, title, methodEntry.getName()), "Callee"),
                        of(LinkUtils.getToMethodHref(collapsedStackFileName2, title, methodEntry.getName()), "Callers")
                ))
                .collect(Collectors.toList());
    }

    private List<String> getHeaderWithoutFirstFile() {
        return List.of("Method", "Self time % (2)", "Callee FG 2", "Callers FG 2");
    }

    private List<List<TableWithLinks.Link>> createWithoutFirstFile(DecimalFormat decimalFormat, List<MethodCompareInfo> methodsToList, Predicate<? super MethodCompareInfo> filter, Comparator<MethodCompareInfo> comparator) {
        return methodsToList.stream()
                .filter(filter)
                .sorted(comparator)
                .map(methodEntry -> List.of(
                        of(methodEntry.getName()),
                        of(getPercent(methodEntry.getSelfPercent2(), decimalFormat)),
                        of(LinkUtils.getFromMethodHref(collapsedStackFileName2, title, methodEntry.getName()), "Callee"),
                        of(LinkUtils.getToMethodHref(collapsedStackFileName2, title, methodEntry.getName()), "Callers")
                ))
                .collect(Collectors.toList());
    }

    private List<String> getHeaderWithoutSecondFile() {
        return List.of("Method", "Self time % (1)", "Callee FG 1", "Callers FG 1");
    }

    private List<List<TableWithLinks.Link>> createWithoutSecondFile(DecimalFormat decimalFormat, List<MethodCompareInfo> methodsToList, Predicate<? super MethodCompareInfo> filter, Comparator<MethodCompareInfo> comparator) {
        return methodsToList.stream()
                .filter(filter)
                .sorted(comparator)
                .map(methodEntry -> List.of(
                        of(methodEntry.getName()),
                        of(getPercent(methodEntry.getSelfPercent1(), decimalFormat)),
                        of(LinkUtils.getFromMethodHref(collapsedStackFileName1, title, methodEntry.getName()), "Callee"),
                        of(LinkUtils.getToMethodHref(collapsedStackFileName1, title, methodEntry.getName()), "Callers")
                ))
                .collect(Collectors.toList());
    }

    private boolean overThreshold(BigDecimal totalCount, MethodCompareInfo methodInfo) {
        return new BigDecimal(methodInfo.getSelfSamples1()).divide(totalCount, 3, RoundingMode.HALF_EVEN).compareTo(threshold) > 0 ||
                new BigDecimal(methodInfo.getSelfSamples2()).divide(totalCount, 3, RoundingMode.HALF_EVEN).compareTo(threshold) > 0;
    }

    private static String getPercent(BigDecimal percent, DecimalFormat decimalFormat) {
        return PageCreatorHelper.numToString(percent, decimalFormat);
    }
}
