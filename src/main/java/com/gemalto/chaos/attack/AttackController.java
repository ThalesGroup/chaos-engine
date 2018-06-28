package com.gemalto.chaos.attack;

import com.google.gson.Gson;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value = "/attack", produces = "application/json; charset=utf-8")
public class AttackController {
    @Autowired
    private AttackManager attackManager;

    @GetMapping
    public String getAttacks () {
        return new Gson().toJson(attackManager.getActiveAttacks());
    }
}
