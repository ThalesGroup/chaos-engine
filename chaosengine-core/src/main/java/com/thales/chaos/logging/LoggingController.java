/*
 *    Copyright (c) 2019 Thales Group
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
