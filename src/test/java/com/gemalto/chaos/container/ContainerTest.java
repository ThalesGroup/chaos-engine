package com.gemalto.chaos.container;

import com.gemalto.chaos.container.enums.ContainerHealth;
import com.gemalto.chaos.experiment.annotations.NetworkExperiment;
import com.gemalto.chaos.experiment.annotations.StateExperiment;
import com.gemalto.chaos.experiment.enums.ExperimentType;
import com.gemalto.chaos.notification.datadog.DataDogIdentifier;
import com.gemalto.chaos.platform.Platform;
import org.hamcrest.collection.IsEmptyIterable;
import org.hamcrest.collection.IsIterableContainingInAnyOrder;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import static org.junit.Assert.*;
import static org.mockito.Mockito.doReturn;

@RunWith(SpringJUnit4ClassRunner.class)
public class ContainerTest {
    @Mock
    private Platform platform;
    private Container testContainer = new Container() {
        private String field1 = "FIELD1";

        @Override
        public Platform getPlatform () {
            return platform;
        }

        @Override
        protected ContainerHealth updateContainerHealthImpl (ExperimentType experimentType) {
            return ContainerHealth.NORMAL;
        }

        @Override
        public String getSimpleName () {
            return null;
        }

        @Override
        public DataDogIdentifier getDataDogIdentifier () {
            return null;
        }
    };
    private Container testContainer2 = new Container() {
        @StateExperiment
        private void nullStateMethod () {
        }

        @NetworkExperiment
        private void nullNetworkMethod () {
        }

        @Override
        public Platform getPlatform () {
            return platform;
        }

        @Override
        protected ContainerHealth updateContainerHealthImpl (ExperimentType experimentType) {
            return ContainerHealth.RUNNING_EXPERIMENT;
        }

        @Override
        public String getSimpleName () {
            return null;
        }

        @Override
        public DataDogIdentifier getDataDogIdentifier () {
            return null;
        }
    };

    @Before
    public void setUp () {
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    @Test
    public void canAttack () {
        // TODO : Mock the random class inside the container.
        doReturn(1D).when(platform).getDestructionProbability();
        assertFalse(testContainer.canExperiment());
        doReturn(0D).when(platform).getDestructionProbability();
        assertFalse(testContainer.canExperiment());

        doReturn(1D).when(platform).getDestructionProbability();
        assertTrue(testContainer2.canExperiment());
        doReturn(0D).when(platform).getDestructionProbability();
        assertFalse(testContainer2.canExperiment());
    }

    @Test
    public void getSupportedAttackTypes () {
        assertThat(testContainer.getSupportedExperimentTypes(), IsEmptyIterable.emptyIterableOf(ExperimentType.class));
        assertThat(testContainer2.getSupportedExperimentTypes(), IsIterableContainingInAnyOrder.containsInAnyOrder(ExperimentType.STATE, ExperimentType.NETWORK));
    }

    @Test
    public void supportsAttackType () {
        assertFalse(testContainer.supportsExperimentType(ExperimentType.STATE));
        assertFalse(testContainer.supportsExperimentType(ExperimentType.NETWORK));
        assertFalse(testContainer.supportsExperimentType(ExperimentType.RESOURCE));
        assertTrue(testContainer2.supportsExperimentType(ExperimentType.STATE));
        assertTrue(testContainer2.supportsExperimentType(ExperimentType.NETWORK));
        assertFalse(testContainer2.supportsExperimentType(ExperimentType.RESOURCE));
    }

    @Test
    public void getContainerHealth () {
        assertEquals(ContainerHealth.NORMAL, testContainer.getContainerHealth(null));
        assertEquals(ContainerHealth.RUNNING_EXPERIMENT, testContainer2.getContainerHealth(null));
    }
}