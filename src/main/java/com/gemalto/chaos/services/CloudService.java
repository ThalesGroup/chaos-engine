package com.gemalto.chaos.services;

import com.gemalto.chaos.container.Container;
import com.gemalto.chaos.container.enums.ContainerHealth;

public interface CloudService {

    void kill(Container container);

    void degrade(Container container);

    ContainerHealth checkHealth(Container container);

}
