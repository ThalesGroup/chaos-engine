package com.gemalto.chaos.health;


import io.netty.handler.codec.http.HttpResponseStatus;
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
    public HttpResponseStatus getHealth() {
        switch (healthManager.getHealth()) {
            case OK:
                return HttpResponseStatus.OK;
            case UNKNOWN:
                return HttpResponseStatus.SERVICE_UNAVAILABLE;
            default:
                return HttpResponseStatus.SERVICE_UNAVAILABLE;
        }

    }

}
