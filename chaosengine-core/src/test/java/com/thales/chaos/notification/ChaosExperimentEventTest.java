package com.thales.chaos.notification;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.thales.chaos.container.Container;
import com.thales.chaos.container.enums.ContainerHealth;
import com.thales.chaos.experiment.Experiment;
import com.thales.chaos.experiment.enums.ExperimentType;
import com.thales.chaos.experiment.impl.GenericContainerExperiment;
import com.thales.chaos.notification.datadog.DataDogIdentifier;
import com.thales.chaos.notification.enums.NotificationLevel;
import com.thales.chaos.notification.message.ChaosExperimentEvent;
import com.thales.chaos.platform.Platform;
import com.thales.chaos.util.StringUtils;
import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import javax.validation.constraints.NotNull;
import java.util.Date;
import java.util.Map;
import java.util.Random;

import static com.thales.chaos.notification.message.ChaosExperimentEvent.CHAOS_EXPERIMENT_EVENT_PREFIX;
import static java.util.UUID.randomUUID;
import static org.hamcrest.collection.IsMapContaining.hasEntry;
import static org.junit.Assert.*;

@RunWith(MockitoJUnitRunner.class)
public class ChaosExperimentEventTest {
    private static final String chaosMessage = "It's chaos time!";
    @Mock
    private Date date;
    private Container exampleContainer;
    private String title = StringUtils.generateRandomString(50);
    private String experimentId = randomUUID().toString();
    private NotificationLevel notificationLevel = NotificationLevel.values()[new Random().nextInt(NotificationLevel.values().length)];
    private ChaosExperimentEvent chaosExperimentEvent;

    @Before
    public void setUp () {
        exampleContainer = new Container() {
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
        chaosExperimentEvent = ChaosExperimentEvent.builder()
                                                   .withChaosTime(date)
                                                   .withTitle(title)
                                                   .withMessage(chaosMessage)
                                                   .withTargetContainer(exampleContainer)
                                                   .withNotificationLevel(notificationLevel)
                                                   .withExperimentId(experimentId)
                                                   .build();
    }

    @Test
    public void asMap () {
        Map<String, Object> resultingMap = chaosExperimentEvent.asMap();
        assertThat(resultingMap, hasEntry("targetContainer", new ObjectMapper().convertValue(exampleContainer, Map.class)));
        assertThat(resultingMap, hasEntry("message", chaosMessage));
        assertThat(resultingMap, hasEntry("chaosTime", 0L));
        assertThat(resultingMap, hasEntry("notificationLevel", notificationLevel.toString()));
        assertThat(resultingMap, hasEntry("experimentId", experimentId));
    }

    @Test
    public void testEventComparison () {
        assertEquals("Identical object must be equal", chaosExperimentEvent, chaosExperimentEvent);
        assertEquals("Identical object must have equal hash code", chaosExperimentEvent.hashCode(), chaosExperimentEvent
                .hashCode());
        ChaosExperimentEvent chaosExperimentEvent2 = ChaosExperimentEvent.builder()
                                                                         .withChaosTime(date)
                                                                         .withTitle(title)
                                                                         .withMessage(chaosMessage)
                                                                         .withTargetContainer(exampleContainer)
                                                                         .withNotificationLevel(notificationLevel)
                                                                         .withExperimentId(experimentId)
                                                                         .build();
        assertEquals("Two events with same fields must be equal", chaosExperimentEvent, chaosExperimentEvent2);
        assertEquals("Two events with same fields must have identical hash code", chaosExperimentEvent.hashCode(), chaosExperimentEvent2
                .hashCode());
        chaosExperimentEvent2 = ChaosExperimentEvent.builder()
                                                    .withChaosTime(date)
                                                    .withTitle(title)
                                                    .withMessage("This is chaos!")
                                                    .withTargetContainer(exampleContainer)
                                                    .withNotificationLevel(notificationLevel)
                                                    .withExperimentId(experimentId)
                                                    .build();
        assertNotEquals("Events with different message cannot be equal", chaosExperimentEvent, chaosExperimentEvent2);
        assertNotEquals("Events with different message must have different hash code", chaosExperimentEvent.hashCode(), chaosExperimentEvent2
                .hashCode());
        chaosExperimentEvent2 = ChaosExperimentEvent.builder()
                                                    .withChaosTime(date)
                                                    .withTitle(title)
                                                    .withMessage(chaosMessage)
                                                    .withTargetContainer(Mockito.mock(Container.class))
                                                    .withNotificationLevel(notificationLevel)
                                                    .withExperimentId(experimentId)
                                                    .build();
        assertNotEquals("Events with different target container cannot be equal", chaosExperimentEvent, chaosExperimentEvent2);
        assertNotEquals("Events with different target container must have different hash code", chaosExperimentEvent.hashCode(), chaosExperimentEvent2
                .hashCode());
        chaosExperimentEvent2 = ChaosExperimentEvent.builder()
                                                    .withChaosTime(date)
                                                    .withTitle(title)
                                                    .withMessage(chaosMessage)
                                                    .withExperimentMethod("ExperimentMethod")
                                                    .withTargetContainer(exampleContainer)
                                                    .withNotificationLevel(notificationLevel)
                                                    .withExperimentId(experimentId)
                                                    .build();
        assertNotEquals("Events with different experiment methods cannot be equal", chaosExperimentEvent, chaosExperimentEvent2);
        assertNotEquals("Events with different experiment methods must have different hash code", chaosExperimentEvent.hashCode(), chaosExperimentEvent2
                .hashCode());
        chaosExperimentEvent2 = ChaosExperimentEvent.builder()
                                                    .withChaosTime(date)
                                                    .withTitle("New Title")
                                                    .withMessage(chaosMessage)
                                                    .withTargetContainer(exampleContainer)
                                                    .withNotificationLevel(notificationLevel)
                                                    .withExperimentId(experimentId)
                                                    .build();
        assertNotEquals("Events with different titles cannot be equal", chaosExperimentEvent, chaosExperimentEvent2);
        assertNotEquals("Events with different titles must have different hash code", chaosExperimentEvent.hashCode(), chaosExperimentEvent2
                .hashCode());
    }

    @Test
    public void testEventFromExperiment () {
        Experiment experiment = GenericContainerExperiment.builder().withContainer(exampleContainer).build();
        ChaosExperimentEvent chaosExperimentEvent = ChaosExperimentEvent.builder().fromExperiment(experiment).build();
        assertThat(experiment.getContainer(), Matchers.is(chaosExperimentEvent.getTargetContainer()));
        assertThat(CHAOS_EXPERIMENT_EVENT_PREFIX, Matchers.is(chaosExperimentEvent.getTitle()));
        assertThat(chaosExperimentEvent.getMessage(), Matchers.isEmptyOrNullString());
    }
}