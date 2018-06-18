package com.gemalto.chaos.health;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HealthController {

    private static final Logger log = LoggerFactory.getLogger(HealthController.class);
    @Autowired
    private HealthManager healthManager;

    HealthController() {
        log.info("Starting the Health Controller endpoint");
    }

    @RequestMapping("/health")
    @GetMapping
    public String getHealth() {
        return healthManager.getHealth().name();
    }
}
