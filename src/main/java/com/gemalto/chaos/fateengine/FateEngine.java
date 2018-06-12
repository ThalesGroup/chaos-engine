package com.gemalto.chaos.fateengine;

import com.gemalto.chaos.container.Container;

public interface FateEngine {

    default boolean canDestroy(Container container) {
        return false;
    }

}
