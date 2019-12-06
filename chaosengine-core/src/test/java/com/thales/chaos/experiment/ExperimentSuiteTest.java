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

package com.thales.chaos.experiment;

import com.thales.chaos.container.Container;
import com.thales.chaos.platform.Platform;
import org.junit.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

public class ExperimentSuiteTest {
    @Test
    public void fromExperiments () {
        final String PLATFORM_TYPE = "complex platform";
        final String CONTAINER_AGGREGATOR = "aggregator";
        final String EXPERIMENT_METHOD_NAME = "destroy";
        Platform platform = mock(Platform.class);
        Experiment experiment = mock(Experiment.class);
        Container container = mock(Container.class);
        doReturn(PLATFORM_TYPE).when(platform).getPlatformType();
        doReturn(container).when(experiment).getContainer();
        doReturn(CONTAINER_AGGREGATOR).when(container).getAggregationIdentifier();
        doReturn(EXPERIMENT_METHOD_NAME).when(experiment).getExperimentMethodName();
        ExperimentSuite generatedExperimentSuite = ExperimentSuite.fromExperiments(platform, Set.of(experiment));
        ExperimentSuite expectedExperimentSuite = new ExperimentSuite(PLATFORM_TYPE, ExperimentSuite.fromMap(Map.of(CONTAINER_AGGREGATOR, List
                .of(EXPERIMENT_METHOD_NAME))));
        assertEquals(expectedExperimentSuite, generatedExperimentSuite);
    }
}