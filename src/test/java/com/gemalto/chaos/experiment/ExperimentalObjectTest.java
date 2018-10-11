package com.gemalto.chaos.experiment;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import static org.junit.Assert.assertFalse;

@RunWith(MockitoJUnitRunner.class)
public class ExperimentalObjectTest {
    private ExperimentalObject experimentalObject;

    @Before
    public void setUp () {
        experimentalObject = new ExperimentalObject() {
        };
    }

    @Test
    public void canAttack () {
        assertFalse(experimentalObject.canExperiment());
    }
}