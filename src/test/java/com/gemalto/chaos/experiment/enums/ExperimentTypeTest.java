package com.gemalto.chaos.experiment.enums;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.Random;

import static org.junit.Assert.assertNotEquals;

@RunWith(MockitoJUnitRunner.class)
public class ExperimentTypeTest {
    @Mock
    private Random random;

    @Test
    public void ExperimentTypeTestImpl () {
        assertNotEquals(ExperimentType.valueOf("NETWORK"), null);
        assertNotEquals(ExperimentType.valueOf("RESOURCE"), null);
        assertNotEquals(ExperimentType.valueOf("STATE"), null);
    }
}