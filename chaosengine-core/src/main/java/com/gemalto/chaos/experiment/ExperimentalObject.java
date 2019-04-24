package com.gemalto.chaos.experiment;

public interface ExperimentalObject {
    default boolean canExperiment () {
        return false;
    }
}
