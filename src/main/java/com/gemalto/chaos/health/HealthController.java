package com.gemalto.chaos.health;


import com.gemalto.chaos.health.enums.SystemHealthState;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/health")
public class HealthController {

    @Autowired
    private HealthManager healthManager;

    @GetMapping
    public SystemHealthState getHealth() {
        return healthManager.getHealth();
    }

}
