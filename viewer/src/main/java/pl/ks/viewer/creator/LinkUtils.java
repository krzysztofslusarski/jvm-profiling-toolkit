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
package pl.ks.viewer.creator;

import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import lombok.experimental.UtilityClass;

@UtilityClass
class LinkUtils {
    String getFlameGraphHref(String collapsed, String title) {
        return "/flame-graph?collapsed=" + collapsed + "&title=" + URLEncoder.encode(title, Charset.defaultCharset());
    }

    String getSkippedFlameGraphHref(String collapsed, String title, int skipped) {
        return "/flame-graph-no-thread?skipped=" + skipped + "&collapsed=" + collapsed + "&title=" + URLEncoder.encode(title, Charset.defaultCharset());
    }

    String getHotspotFlameGraphHref(String collapsed, String title) {
        return "/flame-graph-hotspot?collapsed=" + collapsed + "&title=" + URLEncoder.encode(title, Charset.defaultCharset());
    }

    String getHotspotLLimitedFlameGraphHref(String collapsed, String title, int limit) {
        return "/flame-graph-hotspot-limited?limit=" + limit + "&collapsed=" + collapsed + "&title=" + URLEncoder.encode(title, Charset.defaultCharset());
    }

    String getFromMethodHref(String collapsed, String title, String methodName) {
        return "/from-method?collapsed=" + collapsed + "&method=" + URLEncoder.encode(methodName, Charset.defaultCharset()) +
                "&title=" + URLEncoder.encode(title, Charset.defaultCharset());
    }

    String getFromMethodRootHref(String collapsed, String title, String methodName, BigDecimal totalTimeThreshold, BigDecimal selfTimeThreshold) {
        return "/from-method-root?collapsed=" + collapsed + "&method=" + URLEncoder.encode(methodName, Charset.defaultCharset()) +
                "&totalTimeThreshold=" + totalTimeThreshold + "&selfTimeThreshold=" + selfTimeThreshold +
                "&title=" + URLEncoder.encode(title, Charset.defaultCharset());
    }

    String getToMethodHref(String collapsed, String title, String methodName) {
        return "/to-method?collapsed=" + collapsed + "&method=" + URLEncoder.encode(methodName, Charset.defaultCharset()) +
                "&title=" + URLEncoder.encode(title, Charset.defaultCharset());
    }

    String getToMethodRootHref(String collapsed, String title, String methodName, BigDecimal totalTimeThreshold, BigDecimal selfTimeThreshold) {
        return "/to-method-root?collapsed=" + collapsed + "&method=" + URLEncoder.encode(methodName, Charset.defaultCharset()) +
                "&totalTimeThreshold=" + totalTimeThreshold + "&selfTimeThreshold=" + selfTimeThreshold +
                "&title=" + URLEncoder.encode(title, Charset.defaultCharset());
    }
}
