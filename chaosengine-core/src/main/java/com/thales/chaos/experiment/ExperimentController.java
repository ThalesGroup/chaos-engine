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

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.time.Instant;
import java.util.Collection;
import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping(value = "/experiment", produces = "application/json; charset=utf-8")
@Tag(name = ExperimentController.EXPERIMENT)
public class ExperimentController {
    public static final String EXPERIMENT = "Experiment";
    private static final String MODE_PATH = "/automated";
    private ExperimentManager experimentManager;

    @Autowired
    public ExperimentController (ExperimentManager experimentManager) {
        this.experimentManager = experimentManager;
    }

    @Operation(summary = "Get Experiments", description = "Returns a list of all active experiments.")
    @GetMapping
    public Collection<Experiment> getExperiments () {
        return experimentManager.getAllExperiments();
    }

    @Operation(summary = "Get Specific Experiment",
               description = "Returns metadata of a specific experiment.",
               responses = {
                       @ApiResponse(description = "Return the specific experiment.",
                                    content = @Content(schema = @Schema(implementation = Experiment.class)))
               })
    @GetMapping("/{uuid}")
    public Experiment getExperimentById (
            @Parameter(description = "The UUID of the specific experiment to retrieve metadata off.", required = true)
            @PathVariable String uuid) {
        return experimentManager.getExperimentByUUID(uuid);
    }

    @Operation(summary = "Start Random Experiment",
               description = "Starts a new experiment immediately. This ignores scheduling, and ensures at least one container in the chosen platform is used for experimentation.",
               responses = {
                       @ApiResponse(description = "A set of started experiments.",
                                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = Experiment.class))))
               })
    @PostMapping("/start")
    public Set<Experiment> startExperiments () {
        return experimentManager.scheduleExperiments(true);
    }

    @Operation(summary = "Start Experiment on Specific Container",
               description = "Creates an experiment for a specific container.",
               responses = {
                       @ApiResponse(description = "A set of started experiments.",
                                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = Experiment.class))))
               })
    @PostMapping("/start/{id}")
    public Set<Experiment> experimentContainerWithId (
            @Parameter(description = "The identity value of the container to perform an experiment on.",
                       required = true) @PathVariable long id) {
        return experimentManager.experimentContainerId(id);
    }

    @Operation(summary = "Start a pre-planned experiment",
               description = "Starts an experiment against a specific platform, with specific types of containers running specific experiment methods",
               responses = {
                       @ApiResponse(description = "A set of started experiments.",
                                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = Experiment.class))))
               })
    @PostMapping("/build")
    public Collection<Experiment> startExperimentSuite (@Parameter(required = true,
                                                                   description = "The Experiment Suite object of Platform, Container Aggregation ID, and Experiment Methods")
                                                        @RequestBody ExperimentSuite experimentSuite) {
        return experimentManager.scheduleExperimentSuite(experimentSuite);
    }

    @Operation(summary = "Get parameters for previously run experiments",
               description = "Get historical experiments, in the exact JSON format needed to use the /build endpoint",
               responses = {
                       @ApiResponse(description = "A set of started experiments.")
               })
    @GetMapping("/history")
    public Map<Instant, ExperimentSuite> getHistoricalExperiments () {
        return experimentManager.getHistoricalExperimentSuites();
    }

    @Operation(summary = "Enable Automated Mode",
               description = "Enable automated scheduling of experiments",
               responses = {
                       @ApiResponse(description = "Mode set successfully",
                                    content = @Content(mediaType = "text/plain",
                                                       schema = @Schema(implementation = String.class)))
               })
    @PostMapping(MODE_PATH)
    public String enableAutomatedMode () {
        experimentManager.setAutomatedMode(true);
        return "ok";
    }

    @Operation(summary = "Disable Automated Mode",
               description = "Disable automated scheduling of experiments",
               responses = {
                       @ApiResponse(description = "Mode set successfully",
                                    content = @Content(mediaType = "text/plain",
                                                       schema = @Schema(implementation = String.class)))
               })
    @DeleteMapping(MODE_PATH)
    public String disableAutomatedMode () {
        experimentManager.setAutomatedMode(false);
        return "ok";
    }

    @Operation(summary = "Get Automated Mode Status",
               description = "Returns true if automated mode is enabled",
               responses = {
                       @ApiResponse(description = "Information about automated mode status",
                                    content = @Content(schema = @Schema(implementation = Boolean.class)))
               })
    @GetMapping(MODE_PATH)
    public Boolean isAutomatedMode () {
        return experimentManager.isAutomatedMode();
    }

    @Operation(summary = "Change ExperimentBackoff duration",
               description = "Controls the minimum amount of time between experiments",
               responses = {
                       @ApiResponse(description = "Backoff period set successfully")
               })
    @PatchMapping("/backoff")
    public void setBackoffDuration (@Parameter(required = true,
                                               description = "New minimum backoff period between experiments. Formatted as \"PT[nH][nM][nS]\".",
                                               example = "PT1H15M45S") @RequestParam Duration backoffDuration) {
        experimentManager.setExperimentBackoffPeriod(backoffDuration);
    }
}
