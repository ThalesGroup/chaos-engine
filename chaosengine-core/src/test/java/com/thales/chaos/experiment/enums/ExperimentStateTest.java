package com.thales.chaos.experiment.enums;

import org.junit.Test;

import java.util.Collection;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;

public class ExperimentStateTest {
    @Test
    public void ExperimentStateTestImpl () {
        assertNotNull(ExperimentState.valueOf("FINISHED"));
        assertNotNull(ExperimentState.valueOf("STARTED"));
        assertNotNull(ExperimentState.valueOf("CREATED"));
        assertNotNull(ExperimentState.valueOf("FAILED"));
    }

    @Test
    public void sequentialTestLevels () {
        Collection<ExperimentState> allTestLevels = Stream.iterate(ExperimentState.CREATED, ExperimentState::getNextLevel)
                                                          .takeWhile(Objects::nonNull)
                                                          .collect(Collectors.toList());
        assertThat("Experiment State levels should be continuous", allTestLevels, containsInAnyOrder(ExperimentState.values()));
    }
}