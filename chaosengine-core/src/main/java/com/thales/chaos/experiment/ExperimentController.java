package com.thales.chaos.experiment;

import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Collection;
import java.util.Set;

@RestController
@RequestMapping(value = "/experiment", produces = "application/json; charset=utf-8")
public class ExperimentController {
    private ExperimentManager experimentManager;

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
}
