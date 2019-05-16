package com.thales.chaos.notification;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.thales.chaos.container.Container;
import com.thales.chaos.container.enums.ContainerHealth;
import com.thales.chaos.experiment.enums.ExperimentType;
import com.thales.chaos.notification.datadog.DataDogIdentifier;
import com.thales.chaos.notification.enums.NotificationLevel;
import com.thales.chaos.platform.Platform;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import javax.validation.constraints.NotNull;
import java.util.Date;
import java.util.Map;
import java.util.Random;

import static java.util.UUID.randomUUID;
import static org.hamcrest.collection.IsMapContaining.hasEntry;
import static org.junit.Assert.assertThat;

@RunWith(MockitoJUnitRunner.class)
public class ChaosEventTest {
    private static final String chaosMessage = "It's chaos time!";
    @Mock
    private Container container;
    @Mock
    private Date date;

    @Test
    public void testToString () {
        ChaosEvent chaosEvent = ChaosEvent.builder()
                                          .withChaosTime(date)
                                          .withMessage(chaosMessage)
                                          .withTargetContainer(container)
                                          .build();
        Mockito.when(container.toString()).thenReturn("ChaosEventTestContainer");
        Mockito.when(date.toString()).thenReturn("Chaos-O'Clock");
        String expectedString = "Chaos Event: [targetContainer=ChaosEventTestContainer]" + "[chaosTime=Chaos-O'Clock]" + "[message=It's chaos time!]";
        Assert.assertEquals(expectedString, chaosEvent.toString());
    }

    @Test
    public void asMap () {
        String experimentId = randomUUID().toString();
        NotificationLevel notificationLevel = NotificationLevel.values()[new Random().nextInt(NotificationLevel.values().length)];
        Container exampleContainer = new Container() {
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
        ChaosEvent chaosEvent = ChaosEvent.builder()
                                          .withChaosTime(date)
                                          .withMessage(chaosMessage)
                                          .withTargetContainer(exampleContainer)
                                          .withNotificationLevel(notificationLevel)
                                          .withExperimentId(experimentId)
                                          .build();
        Map<Object, Object> resultingMap = chaosEvent.asMap();
        assertThat(resultingMap, hasEntry("targetContainer", new ObjectMapper().convertValue(exampleContainer, Map.class)));
        assertThat(resultingMap, hasEntry("message", chaosMessage));
        assertThat(resultingMap, hasEntry("chaosTime", 0L));
        assertThat(resultingMap, hasEntry("notificationLevel", notificationLevel.toString()));
        assertThat(resultingMap, hasEntry("experimentId", experimentId));
    }
}