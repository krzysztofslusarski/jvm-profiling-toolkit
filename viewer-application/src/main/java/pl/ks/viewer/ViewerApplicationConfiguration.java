package pl.ks.viewer;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
@ComponentScan({
        "pl.ks.viewer",
        "pl.ks.jfr",
})
class ViewerApplicationConfiguration {
}
