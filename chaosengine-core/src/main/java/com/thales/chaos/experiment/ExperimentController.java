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

package com.thales.chaos.experiment;

import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.time.Instant;
import java.util.Collection;
import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping(value = "/experiment", produces = "application/json; charset=utf-8")
public class ExperimentController {
    private ExperimentManager experimentManager;
    private static final String MODE_PATH = "/automated";

    @Autowired
    public ExperimentController (ExperimentManager experimentManager) {
        this.experimentManager = experimentManager;
    }

    @ApiOperation(value = "Get Experiments", notes = "Returns a list of all active experiments.")
    @GetMapping
    public Collection<Experiment> getExperiments () {
        return experimentManager.getAllExperiments();
    }

    @ApiOperation(value = "Get Specific Experiment", notes = "Returns metadata of a specific experiment.")
    @GetMapping("/{uuid}")
    public Experiment getExperimentById (@ApiParam(value = "The UUID of the specific experiment to retrieve metadata off.", required = true) @PathVariable String uuid) {
        return experimentManager.getExperimentByUUID(uuid);
    }

    @ApiOperation(value = "Start Random Experiment", notes = "Starts a new experiment immediately. This ignores scheduling, and ensures at least one container in the chosen platform is used for experimentation.")
    @PostMapping("/start")
    public Set<Experiment> startExperiments () {
        return experimentManager.scheduleExperiments(true);
    }

    @ApiOperation(value = "Start Experiment on Specific Container", notes = "Creates an experiment for a specific container.")
    @PostMapping("/start/{id}")
    public Set<Experiment> experimentContainerWithId (@ApiParam(value = "The identity value of the container to perform an experiment on.", required = true) @PathVariable long id) {
        return experimentManager.experimentContainerId(id);
    }

    @ApiOperation(value = "Start a pre-planned experiment", notes = "Starts an experiment against a specific platform, with specific types of containers running specific experiment methods")
    @PostMapping("/build")
    public Collection<Experiment> startExperimentSuite (@ApiParam(required = true, value = "The Experiment Suite object of Platform, Container Aggregation ID, and Experiment Methods") @RequestBody ExperimentSuite experimentSuite) {
        return experimentManager.scheduleExperimentSuite(experimentSuite);
    }

    @ApiOperation(value = "Get parameters for previously run experiments", notes = "Get historical experiments, in the exact JSON format needed to use the /build endpoint")
    @GetMapping("/history")
    public Map<Instant, ExperimentSuite> getHistoricalExperiments () {
        return experimentManager.getHistoricalExperimentSuites();
    }

    @ApiOperation(value = "Enable Automated Mode", notes = "Enable automated scheduling of experiments ")
    @PostMapping(MODE_PATH)
    public String enableAutomatedMode () {
        experimentManager.setAutomatedMode(true);
        return "ok";
    }

    @ApiOperation(value = "Disable Automated Mode", notes = "Disable automated scheduling of experiments ")
    @DeleteMapping(MODE_PATH)
    public String disableAutomatedMode () {
        experimentManager.setAutomatedMode(false);
        return "ok";
    }

    @ApiOperation(value = "Get Automated Mode Status", notes = "Returns true if automated mode is enabled")
    @GetMapping(MODE_PATH)
    public Boolean isAutomatedMode () {
        return experimentManager.isAutomatedMode();
    }

    @ApiOperation(value = "Change ExperimentBackoff duration", notes = "Controls the minimum amount of time between experiments")
    @PatchMapping("/backoff")
    public void setBackoffDuration (@ApiParam(required = true, value = "New minimum backoff period between experiments. Formatted as \"PT[nH][nM][nS]\".", example = "PT1H15M45S") @RequestParam Duration backoffDuration) {
        experimentManager.setExperimentBackoffPeriod(backoffDuration);
    }
}
