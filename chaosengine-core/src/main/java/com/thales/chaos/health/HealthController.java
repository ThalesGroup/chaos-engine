package com.thales.chaos.health;

import com.thales.chaos.health.enums.SystemHealthState;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/health")
public class HealthController {
    @Autowired
    private HealthManager healthManager;

    @Autowired
    HealthController () {
    }

    @ApiOperation(value = "Health Check Endpoint", notes = "Returns HTTP 200 if the system believes it is healthy, or HTTP 5xx errors otherwise.")
    @GetMapping
    public SystemHealthState getHealth () {
        switch (healthManager.getHealth()) {
            case OK:
                return SystemHealthState.OK;
            case ERROR:
                throw new HealthErrorException();
            case UNKNOWN:
            default:
                throw new HealthUnknownException();
        }
    }

    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    private class HealthErrorException extends RuntimeException {
    }

    @ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
    private class HealthUnknownException extends RuntimeException {
    }
}
