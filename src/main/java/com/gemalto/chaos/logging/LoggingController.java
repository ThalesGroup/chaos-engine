package com.gemalto.chaos.logging;

import ch.qos.logback.classic.Level;
import com.gemalto.chaos.logging.enums.LoggingLevel;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/logging")
public class LoggingController {
    private static final String GEMALTO_CLASS = "com.gemalto";

    @PostMapping
    public String setLogLevel (@RequestParam LoggingLevel loggingLevel) {
        setLogLevel(loggingLevel, GEMALTO_CLASS);
        return "ok";
    }

    @PostMapping("/{loggingClass}")
    public String setLogLevel (@RequestParam LoggingLevel loggingLevel, @PathVariable String loggingClass) {
        ((ch.qos.logback.classic.Logger) LoggerFactory.getLogger(loggingClass)).setLevel(Level.valueOf(loggingLevel.toString()));
        return "ok";
    }

    @GetMapping
    public String getLogLevel () {
        return getLogLevel(GEMALTO_CLASS);
    }

    @GetMapping("/{loggingClass}")
    public String getLogLevel (@PathVariable String loggingClass) {
        return ((ch.qos.logback.classic.Logger) LoggerFactory.getLogger(loggingClass)).getLevel().toString();
    }
}
