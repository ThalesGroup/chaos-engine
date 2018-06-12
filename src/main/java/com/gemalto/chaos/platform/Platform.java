package com.gemalto.chaos.platform;

import com.gemalto.chaos.container.Container;

import java.util.List;

public interface Platform {

    void destroy(Container container);

    void degrade(Container container);

    List<Container> getRoster();
}
