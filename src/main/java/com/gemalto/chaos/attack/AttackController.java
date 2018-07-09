package com.gemalto.chaos.attack;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Queue;
import java.util.Set;

@RestController
@RequestMapping(value = "/attack", produces = "application/json; charset=utf-8")
public class AttackController {
    @Autowired
    private AttackManager attackManager;

    @GetMapping
    public Set<Attack> getAttacks () {
        return attackManager.getActiveAttacks();
    }

    @GetMapping("/queue")
    public Queue<Attack> getAttackQueue () {
        return attackManager.getNewAttackQueue();
    }

    @PostMapping("/start")
    public void startAttacks () {
        attackManager.startAttacks();
    }

    @PostMapping("/start/{id}")
    public Set<Attack> attackContainerWithId (@PathVariable long id) {
        return attackManager.attackContainerId(id);
    }
}
