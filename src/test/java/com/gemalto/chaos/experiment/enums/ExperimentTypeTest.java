package com.gemalto.chaos.experiment.enums;

import com.gemalto.chaos.experiment.annotations.NetworkExperiment;
import com.gemalto.chaos.experiment.annotations.ResourceExperiment;
import com.gemalto.chaos.experiment.annotations.StateExperiment;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.beans.factory.annotation.Autowired;

import java.lang.annotation.Annotation;
import java.util.Random;

import static org.junit.Assert.*;

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

    @Test
    public void isExperiment () {
        Annotation state = () -> StateExperiment.class;
        Annotation network = () -> NetworkExperiment.class;
        Annotation resource = () -> ResourceExperiment.class;
        Annotation autowired = () -> Autowired.class;
        assertTrue(ExperimentType.isExperiment(state));
        assertTrue(ExperimentType.isExperiment(network));
        assertTrue(ExperimentType.isExperiment(resource));
        assertFalse(ExperimentType.isExperiment(autowired));
    }
}