package com.gemalto.chaos.health;


import com.gemalto.chaos.health.enums.SystemHealthState;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/health")
public class HealthController {

    @Autowired
    private HealthManager healthManager;

    @GetMapping
    public ResponseEntity<?> getHealth() {
        switch (healthManager.getHealth()) {
            case OK:
                return new ResponseEntity<>(SystemHealthState.OK, HttpStatus.OK);
            case ERROR:
                return new ResponseEntity<>(SystemHealthState.ERROR, HttpStatus.INTERNAL_SERVER_ERROR);
            case UNKNOWN:
            default:
                return new ResponseEntity<>(SystemHealthState.UNKNOWN, HttpStatus.SERVICE_UNAVAILABLE);
        }

    }

}
