package com.gemalto.chaos.attack;

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

    @GetMapping
    public Set<Attack> getAttacks () {
        return attackManager.getActiveAttacks();
    }

    @GetMapping("/{uuid}")
    public Attack getAttackById (@PathVariable String uuid) {
        return attackManager.getAttackByUUID(uuid);
    }

    @GetMapping("/queue")
    public Queue<Attack> getAttackQueue () {
        return attackManager.getNewAttackQueue();
    }

    @PostMapping("/start")
    public void startAttacks () {
        attackManager.startAttacks(true);
    }

    @PostMapping("/start/{id}")
    public Set<Attack> attackContainerWithId (@PathVariable long id) {
        return attackManager.attackContainerId(id);
    }
}
