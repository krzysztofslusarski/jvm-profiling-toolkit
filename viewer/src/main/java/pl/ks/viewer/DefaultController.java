package pl.ks.viewer;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
class DefaultController {
    @GetMapping("/")
    String defaultPage() {
        return "index";
    }
}
