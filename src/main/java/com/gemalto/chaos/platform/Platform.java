package com.gemalto.chaos.platform;

import com.gemalto.chaos.container.Container;
import com.gemalto.chaos.container.enums.ContainerHealth;

import java.util.List;

public interface Platform {

    void destroy(Container container);

    void degrade(Container container);

    ContainerHealth getHealth(Container container);

    List<Container> getRoster();
}
