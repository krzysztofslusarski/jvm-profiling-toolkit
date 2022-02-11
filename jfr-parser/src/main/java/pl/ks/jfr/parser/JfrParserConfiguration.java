package pl.ks.jfr.parser;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
class JfrParserConfiguration {
    @Bean
    JfrParser jfrParser() {
        return new JfrParserImpl();
    }
}
