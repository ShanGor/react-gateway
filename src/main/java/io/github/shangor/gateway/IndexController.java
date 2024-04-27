package io.github.shangor.gateway;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
public class IndexController {
    @GetMapping("/health")
    public String health() {
        return "ok";
    }
}
