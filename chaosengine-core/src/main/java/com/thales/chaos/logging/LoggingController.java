package com.thales.chaos.logging;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import com.thales.chaos.logging.enums.LoggingLevel;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.slf4j.LoggerFactory;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/logging")
public class LoggingController {
    private static final String GEMALTO_CLASS = "com.thales";
    private static final Marker ALWAYS = MarkerFactory.getMarker("ALWAYS");

    @ApiOperation(value = "Set Base Logging Level", notes = "Sets the logging level for the com.thales class.")
    @PostMapping
    public String setLogLevel (@ApiParam(value = "The new logging level to use", required = true) @RequestParam LoggingLevel loggingLevel) {
        setLogLevel(loggingLevel, GEMALTO_CLASS);
        return "ok";
    }

    @ApiOperation(value = "Set Class Logging Level", notes = "Sets the logging level for a specific Java Class Path.")
    @PostMapping("/{loggingClass}")
    public String setLogLevel (@ApiParam(value = "The new logging level to use") @RequestParam LoggingLevel loggingLevel, @ApiParam(value = "The Java Class Path to get the logging level of (i.e., com.thales, com.thales.chaos.platform, org.springframework)") @PathVariable String loggingClass) {
        Logger logger = (Logger) LoggerFactory.getLogger(loggingClass);
        logger.setLevel(Level.valueOf(loggingLevel.toString()));
        logger.info(ALWAYS, "Logging level changed to {}", loggingLevel);
        return "ok";
    }

    @ApiOperation(value = "Get Base Logging Level", notes = "Gets the logging level for the com.thales class.")
    @GetMapping
    public String getLogLevel () {
        return getLogLevel(GEMALTO_CLASS);
    }

    @ApiOperation(value = "Get Class Logging Level", notes = "Gets the logging level for a specific Java Class Path.")
    @GetMapping("/{loggingClass}")
    public String getLogLevel (@ApiParam(value = "The Java Class Path to get the logging level of (i.e., com.thales, com.thales.chaos.platform, org.springframework)") @PathVariable String loggingClass) {
        return ((ch.qos.logback.classic.Logger) LoggerFactory.getLogger(loggingClass)).getLevel().toString();
    }
}
