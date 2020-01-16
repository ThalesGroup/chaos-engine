/*
 *    Copyright (c) 2018 - 2020, Thales DIS CPL Canada, Inc
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 *
 */

package com.thales.chaos.logging;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;

@RestController
@RequestMapping("/logging")
@Tag(name = LoggingController.LOGGING)
public class LoggingController {
    public static final String LOGGING = "Logging";
    @Autowired
    private LoggingManager loggingManager;

    @Operation(summary = "Enable Debug Mode",
               description = "Increases the log level for Chaos Engine internal classes to DEBUG",
               responses = {
                       @ApiResponse(content = @Content(mediaType = "text/plain",
                                                       schema = @Schema(implementation = String.class)))
               })
    @PostMapping({ "", "/{timeout}" })
    public String setDebugMode (@PathVariable(required = false)
                                @Parameter(description = "Duration before automatically reverting logs. Formatted as \"PT[nH][nM][nS]\".",
                                           example = "PT1H15M45S") Duration timeout) {
        if (timeout == null) {
            loggingManager.setDebugMode();
        } else {
            loggingManager.setDebugMode(timeout);
        }
        return "ok";
    }

    @Operation(summary = "Disable Debug Mode",
               description = "Removes DEBUG log level from Chaos Engine internal classes.",
               responses = {
                       @ApiResponse(content = @Content(mediaType = "text/plain",
                                                       schema = @Schema(implementation = String.class)))
               })
    @DeleteMapping
    public String clearDebugMode () {
        loggingManager.clearDebugMode();
        return "ok";
    }
}
