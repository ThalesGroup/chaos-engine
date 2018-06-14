package com.gemalto.chaos.fateengine;

public interface FateEngine {

    default boolean canDestroy() {
        return false;
    }

}
