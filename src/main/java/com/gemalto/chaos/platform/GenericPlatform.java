package com.gemalto.chaos.platform;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class GenericPlatform implements Platform {

    private static final Logger log = LoggerFactory.getLogger(GenericPlatform.class);

    public GenericPlatform() {
        log.info("Created a Generic Platform. This platform acts as a placeholder to ensure a minimum of one autowired platform.");

    }
}
