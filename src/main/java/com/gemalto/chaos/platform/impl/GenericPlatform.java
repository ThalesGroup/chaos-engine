package com.gemalto.chaos.platform.impl;

import com.gemalto.chaos.container.Container;
import com.gemalto.chaos.platform.Platform;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
@ConditionalOnProperty("generic.platform")
public class GenericPlatform implements Platform {

    private static final Logger log = LoggerFactory.getLogger(GenericPlatform.class);

    public GenericPlatform() {
        log.info("Created a Generic Platform. This platform acts as a placeholder to ensure a minimum of one autowired platform.");
    }

    @Override
    public void destroy(Container container) {
        log.warn("Cannot destroy a container, this is a generic platform.");
    }

    @Override
    public void degrade(Container container) {
        log.warn("Cannot degrade a container, this is a generic Platform.");
    }

    @Override
    public List<Container> getRoster() {
        log.warn("Cannot return a list of containers, this is a generic platform");
        return new ArrayList<>();
    }
}
