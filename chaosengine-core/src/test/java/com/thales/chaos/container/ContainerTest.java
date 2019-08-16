package com.thales.chaos.container;

import com.thales.chaos.container.annotations.Identifier;
import com.thales.chaos.container.enums.ContainerHealth;
import com.thales.chaos.experiment.annotations.ChaosExperiment;
import com.thales.chaos.experiment.enums.ExperimentType;
import com.thales.chaos.notification.datadog.DataDogIdentifier;
import com.thales.chaos.platform.Platform;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import javax.validation.constraints.NotNull;

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
        public String getAggregationIdentifier () {
            return null;
        }

        @Override
        public DataDogIdentifier getDataDogIdentifier () {
            return null;
        }

        @Override
        protected boolean compareUniqueIdentifierInner (@NotNull String uniqueIdentifier) {
            return false;
        }
    };
    private Container testContainer2 = new Container() {
        @ChaosExperiment(experimentType = ExperimentType.STATE)
        private void nullStateMethod () {
        }

        @ChaosExperiment(experimentType = ExperimentType.NETWORK)
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
        public String getAggregationIdentifier () {
            return null;
        }

        @Override
        public DataDogIdentifier getDataDogIdentifier () {
            return null;
        }

        @Override
        protected boolean compareUniqueIdentifierInner (@NotNull String uniqueIdentifier) {
            return false;
        }
    };

    @Before
    public void setUp () {
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    @Test
    public void canExperiment () {
        doReturn(1D).when(platform).getDestructionProbability();
        assertTrue(testContainer2.canExperiment());
        doReturn(0D).when(platform).getDestructionProbability();
        assertFalse(testContainer2.canExperiment());
    }

    @Test
    public void getContainerHealth () {
        assertEquals(ContainerHealth.NORMAL, testContainer.getContainerHealth(null));
        assertEquals(ContainerHealth.RUNNING_EXPERIMENT, testContainer2.getContainerHealth(null));
    }

    @Test
    public void getIdentityWithUnorderedFields () {
        /*
        This test validates that the identifier fields are evaluated in a consistent order. The names of the fields
        control the order that Identifier fields are parsed if they have identical `order` values.
         */
        Container badlyConstructedContainer = new Container() {
            @Identifier
            private String field1 = "firstField";
            @Identifier
            private String field2 = "secondField";
            @Identifier
            private String field3 = "thirdField";

            @Override
            public Platform getPlatform () {
                return null;
            }

            @Override
            protected ContainerHealth updateContainerHealthImpl (ExperimentType experimentType) {
                return null;
            }

            @Override
            public String getSimpleName () {
                return null;
            }

            @Override
            public String getAggregationIdentifier () {
                return null;
            }

            @Override
            public DataDogIdentifier getDataDogIdentifier () {
                return null;
            }

            @Override
            protected boolean compareUniqueIdentifierInner (@NotNull String uniqueIdentifier) {
                return false;
            }
        };
        assertEquals(2890670807L, badlyConstructedContainer.getIdentity());
    }
}