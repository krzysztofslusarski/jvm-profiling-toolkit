package pl.ks.profiling;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
@ComponentScan(basePackages = {
        "pl.ks"
})
class ApplicationConfiguration {
}
