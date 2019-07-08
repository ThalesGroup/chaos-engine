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
        ExperimentSuite expectedExperimentSuite = new ExperimentSuite(PLATFORM_TYPE, Map.of(CONTAINER_AGGREGATOR, List.of(EXPERIMENT_METHOD_NAME)));
        assertEquals(expectedExperimentSuite, generatedExperimentSuite);
    }

    @Test
    public void toStringTest () {
        ExperimentSuite experimentSuite;
        experimentSuite = new ExperimentSuite("chosen platform", Map.of("container", List.of("delete", "restart", "blow up")));
        assertEquals("{\"platformType\":\"chosen platform\",\"aggregationIdentifierToExperimentMethodsMap\":{\"container\":[\"delete\",\"restart\",\"blow up\"]}}", experimentSuite
                .toString());
    }
}