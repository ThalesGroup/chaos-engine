package com.gemalto.chaos.platform;

import com.gemalto.chaos.ChaosException;
import com.gemalto.chaos.container.Container;

import java.util.List;

public interface Platform {

    void destroy(Container container) throws ChaosException;

    void degrade(Container container) throws ChaosException;

    List<Container> getRoster();
}
