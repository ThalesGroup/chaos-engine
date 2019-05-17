package com.thales.chaos.experiment.enums;

import org.junit.Test;

import static org.junit.Assert.assertNotEquals;

public class ExperimentStateTest {
    @Test
    public void ExperimentStateTestImpl () {
        assertNotEquals(ExperimentState.valueOf("FINISHED"), null);
        assertNotEquals(ExperimentState.valueOf("STARTED"), null);
        assertNotEquals(ExperimentState.valueOf("NOT_YET_STARTED"), null);
        assertNotEquals(ExperimentState.valueOf("FAILED"), null);
    }
}