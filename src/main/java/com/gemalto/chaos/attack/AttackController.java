package com.gemalto.chaos.attack;

import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Queue;
import java.util.Set;

@RestController
@RequestMapping(value = "/attack", produces = "application/json; charset=utf-8")
public class AttackController {
    private AttackManager attackManager;

    @Autowired
    public AttackController (AttackManager attackManager) {
        this.attackManager = attackManager;
    }

    @ApiOperation(value = "Get Experiments", notes = "Returns a list of all active experiments.")
    @GetMapping
    public Set<Attack> getAttacks () {
        return attackManager.getActiveAttacks();
    }

    @ApiOperation(value = "Get Specific Experiment", notes = "Returns metadata of a specific experiment.")
    @GetMapping("/{uuid}")
    public Attack getAttackById (@ApiParam(value = "The UUID of the specific attack to retrieve metadata off.", required = true) @PathVariable String uuid) {
        return attackManager.getAttackByUUID(uuid);
    }

    @ApiOperation(value = "Get Experiment Queue", notes = "Returns metadata about all experiments queued up to start at the next experiment start interval.")
    @GetMapping("/queue")
    public Queue<Attack> getAttackQueue () {
        return attackManager.getNewAttackQueue();
    }

    @ApiOperation(value = "Start Random Experiment", notes = "Starts a new experiment immediately. This ignores scheduling, and ensures at least one container in the chosen platform is used for experimentation.")
    @PostMapping("/start")
    public Queue<Attack> startAttacks () {
        return attackManager.startAttacks(true);
    }

    @ApiOperation(value = "Start Experiment on Specific Container", notes = "Creates an experiment for a specific container.")
    @PostMapping("/start/{id}")
    public Set<Attack> attackContainerWithId (@ApiParam(value = "The identity value of the container to perform an experiment on.", required = true) @PathVariable long id) {
        return attackManager.attackContainerId(id);
    }
}
