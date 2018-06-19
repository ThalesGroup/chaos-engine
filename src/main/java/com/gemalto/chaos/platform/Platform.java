package com.gemalto.chaos.platform;

import com.gemalto.chaos.container.Container;
import com.gemalto.chaos.container.enums.ContainerHealth;
import com.gemalto.chaos.platform.enums.ApiStatus;

import java.util.List;

public interface Platform {

    ContainerHealth getHealth(Container container);

    List<Container> getRoster();

    ApiStatus getApiStatus();
}
