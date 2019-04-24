package com.gemalto.chaos.notification;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gemalto.chaos.container.Container;
import com.gemalto.chaos.container.impl.AwsEC2Container;
import com.gemalto.chaos.notification.enums.NotificationLevel;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.Date;
import java.util.Map;
import java.util.Random;

import static java.util.UUID.randomUUID;
import static org.hamcrest.collection.IsMapContaining.hasEntry;
import static org.hamcrest.collection.IsMapContaining.hasKey;
import static org.hamcrest.core.IsNot.not;
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
        String instanceId = randomUUID().toString();
        String name = randomUUID().toString();
        String keyName = randomUUID().toString();
        String experimentId = randomUUID().toString();
        NotificationLevel notificationLevel = NotificationLevel.values()[new Random().nextInt(NotificationLevel.values().length)];
        Container exampleContainer = AwsEC2Container.builder()
                                                    .instanceId(instanceId)
                                                    .name(name)
                                                    .keyName(keyName)
                                                    .build();
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
        assertThat(resultingMap, not(hasEntry("instanceId", instanceId)));
        assertThat(resultingMap, not(hasEntry("name", name)));
        assertThat(resultingMap, not(hasEntry("keyName", keyName)));
        assertThat(resultingMap, not(hasKey("randomKey")));
    }
}