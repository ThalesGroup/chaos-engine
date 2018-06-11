package com.gemalto.chaos.platform;

import com.gemalto.chaos.container.Container;

import java.util.List;

public interface Platform {

    void destroy(Container container) throws RuntimeException;

    void degrade(Container container) throws RuntimeException;

    List<Container> getRoster();
}
