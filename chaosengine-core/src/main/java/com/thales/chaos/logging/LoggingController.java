package com.thales.chaos.logging;

import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;

@RestController
@RequestMapping("/logging")
public class LoggingController {
    @Autowired
    private LoggingManager loggingManager;

    @ApiOperation(value = "Enable Debug Mode", notes = "Increases the log level for Thales classes to DEBUG")
    @PostMapping({ "", "/{timeout}" })
    public String setDebugMode (@PathVariable(required = false) @ApiParam(value = "Duration before automatically reverting logs. Formatted as \"PT[nH][nM][nS]\".", example = "PT1H15M45S") Duration timeout) {
        if (timeout == null) {
            loggingManager.setDebugMode();
        } else {
            loggingManager.setDebugMode(timeout);
        }
        return "ok";
    }

    @ApiOperation(value = "Disable Debug Mode", notes = "Increases the log level for Thales classes to DEBUG")
    @DeleteMapping
    public String clearDebugMode () {
        loggingManager.clearDebugMode();
        return "ok";
    }
}
