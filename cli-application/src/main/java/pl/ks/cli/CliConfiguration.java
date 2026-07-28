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

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import pl.ks.jfr.parser.JfrParser;
import pl.ks.viewer.StatefulJfrViewerService;
import pl.ks.viewer.flamegraph.FlameGraphExecutor;

/**
 * The console application reuses the very same parsing, filtering and aggregation as the web viewer - only the
 * rendering differs, so the web controllers and templates are left out of the component scan.
 */
@Configuration(proxyBeanMethods = false)
class CliConfiguration {
    @Bean
    FlameGraphExecutor flameGraphExecutor() {
        return new FlameGraphExecutor();
    }

    @Bean
    StatefulJfrViewerService statefulJfrViewerService(JfrParser jfrParser, FlameGraphExecutor flameGraphExecutor) {
        return new StatefulJfrViewerService(jfrParser, flameGraphExecutor);
    }
}
