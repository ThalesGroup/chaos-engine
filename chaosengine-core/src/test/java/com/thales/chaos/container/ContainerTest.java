/*
 *    Copyright (c) 2019 Thales Group
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 *
 */

package com.thales.chaos.container;

import com.thales.chaos.container.annotations.Identifier;
import com.thales.chaos.container.enums.ContainerHealth;
import com.thales.chaos.exception.ChaosException;
import com.thales.chaos.experiment.Experiment;
import com.thales.chaos.experiment.ExperimentMethod;
import com.thales.chaos.experiment.annotations.ChaosExperiment;
import com.thales.chaos.experiment.enums.ExperimentType;
import com.thales.chaos.notification.datadog.DataDogIdentifier;
import com.thales.chaos.platform.Platform;
import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import javax.validation.constraints.NotNull;

import static com.thales.chaos.container.enums.ContainerHealth.RUNNING_EXPERIMENT;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

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
            return RUNNING_EXPERIMENT;
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
        assertEquals(RUNNING_EXPERIMENT, testContainer2.getContainerHealth(null));
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

    @Test
    public void toStringTest () {
        Container containerWithFields = new MostlyAbstractContainer() {
            @Identifier(order = 0)
            private String name = "container name";
            @Identifier(order = 1)
            private int index = 0;
        };
        assertEquals("Container type: \n" + "\tname:\tcontainer name\n" + "\tindex:\t0", containerWithFields.toString());
    }

    @Test
    public void equalsTest () {
        Container firstEqualContainer = new EqualityTestContainer("containerName", 0);
        Container secondEqualContainer = new EqualityTestContainer("containerName", 0);
        Container unequalContainer = new EqualityTestContainer("containerName", 1);

        /* IntelliJ is suggesting that these be replaced from assertTrue/False with assert(Not)Equals.
        This cannot be done because we specifically want to call the equals method. Using assertions does
        a significant amount of sanitizing beforehand, which doesn't reach the equals method.
         */
        assertEquals(firstEqualContainer, secondEqualContainer);
        assertFalse(firstEqualContainer.equals(unequalContainer));
        assertFalse(secondEqualContainer.equals(unequalContainer));
        assertFalse(firstEqualContainer.equals("A string"));
        assertFalse(firstEqualContainer.equals(null));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void startExperiment () {
        ExperimentMethod experimentMethod = mock(ExperimentMethod.class);
        Experiment experiment = mock(Experiment.class);
        doReturn(experimentMethod).when(experiment).getExperimentMethod();
        testContainer.startExperiment(experiment);
        verify(experimentMethod).accept(testContainer, experiment);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void startExperimentException () {
        ExperimentMethod experimentMethod = mock(ExperimentMethod.class);
        Experiment experiment = mock(Experiment.class);
        RuntimeException experimentException = new RuntimeException();
        doReturn(experimentMethod).when(experiment).getExperimentMethod();
        doThrow(experimentException).when(experimentMethod).accept(testContainer, experiment);
        try {
            testContainer.startExperiment(experiment);
            fail("Exception expected");
        } catch (ChaosException e) {
            verify(experimentMethod).accept(testContainer, experiment);
            assertSame(experimentException, e.getCause());
            assertThat(e.getMessage(), Matchers.startsWith("12001"));
        }
    }

    private class EqualityTestContainer extends MostlyAbstractContainer {
        @Identifier(order = 0)
        private String name;
        @Identifier(order = 1)
        private int index;

        EqualityTestContainer (String name, int index) {
            this.name = name;
            this.index = index;
        }
    }

    private abstract class MostlyAbstractContainer extends Container {
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
    }
}