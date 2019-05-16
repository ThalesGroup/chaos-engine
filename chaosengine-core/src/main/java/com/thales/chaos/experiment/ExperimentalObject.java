package com.thales.chaos.experiment;

public interface ExperimentalObject {
    default boolean canExperiment () {
        return false;
    }
}
