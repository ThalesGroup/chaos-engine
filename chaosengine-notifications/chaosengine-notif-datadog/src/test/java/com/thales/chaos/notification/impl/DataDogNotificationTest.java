package com.thales.chaos.notification.impl;

import com.thales.chaos.container.Container;
import com.thales.chaos.container.enums.ContainerHealth;
import com.thales.chaos.experiment.enums.ExperimentType;
import com.thales.chaos.notification.datadog.DataDogIdentifier;
import com.thales.chaos.notification.enums.NotificationLevel;
import com.thales.chaos.notification.message.ChaosExperimentEvent;
import com.thales.chaos.notification.message.ChaosMessage;
import com.thales.chaos.platform.Platform;
import com.thales.chaos.util.StringUtils;
import com.timgroup.statsd.Event;
import com.timgroup.statsd.StatsDClient;
import com.timgroup.statsd.StatsDClientException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import javax.validation.constraints.NotNull;
import java.time.Instant;
import java.util.*;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.*;

@RunWith(SpringJUnit4ClassRunner.class)
public class DataDogNotificationTest {
    private ChaosExperimentEvent chaosExperimentEvent;
    private ChaosMessage chaosMessage;
    @Mock
    private Platform platform;
    private DataDogNotification.DataDogEvent dataDogEvent ;

    private String experimentId=UUID.randomUUID().toString();
    private String experimentMethod=UUID.randomUUID().toString();
    private String message = StringUtils.generateRandomString(50);
    private String title = StringUtils.generateRandomString(50);
    private String target = UUID.randomUUID().toString();
    private String platformType = "Platform";
    private String conatainerType = "Container";
    private String aggregationIdentifier = conatainerType;
    private Date chaosTime = Date.from(Instant.now());


    private NotificationLevel level = NotificationLevel.WARN;
    private List<String> expectedTagsEvent = new ArrayList<>();
    private List<String> expectedTagsMessage = new ArrayList<>();
    private Container container = new Container() {
        @Override
        public String getContainerType () {
            return conatainerType;
        }
        @Override
        public Platform getPlatform () {
            return platform;
        }

        @Override
        protected ContainerHealth updateContainerHealthImpl (ExperimentType experimentType) {
            return null;
        }

        @Override
        public String getSimpleName () {
            return target;
        }

        @Override
        public String getAggregationIdentifier () {
            return aggregationIdentifier;
        }

        @Override
        public DataDogIdentifier getDataDogIdentifier () {
            return null;
        }

        @Override
        protected boolean compareUniqueIdentifierInner (@NotNull String uniqueIdentifier) {
            return false;
        }

        @Override
        public Map<String, Boolean> getShellCapabilities () {
            Map<String, Boolean> capabilities = Map.of("bash", true, "/path/to/some/binary", false);
            return capabilities;
        }

        @Override
        public Collection<String> getKnownMissingCapabilities () {
            return List.of("/path/to/some/binary");
        }
    };
    @Before
    public void setUp () {
        chaosExperimentEvent = ChaosExperimentEvent.builder()
                                                   .withMessage(message)
                                                   .withExperimentId(experimentId)
                                                   .withNotificationLevel(level)
                                                   .withTargetContainer(container)
                                                   .withExperimentMethod(experimentMethod)
                                                   .withExperimentType(ExperimentType.STATE).withChaosTime(chaosTime)
                                                   .build();
        when(platform.getPlatformType()).thenReturn(platformType);
        dataDogEvent = new DataDogNotification().new DataDogEvent();
        container.getShellCapabilities()
                 .forEach((key, value) -> expectedTagsEvent.add("container.shellCapabilities." + key + ":" + value));
        expectedTagsEvent.add("container.containerType:" + container.getContainerType());
        expectedTagsEvent.add("container.aggregationIdentifier:" + aggregationIdentifier);
        expectedTagsEvent.add("container.simpleName:" + target);
        expectedTagsEvent.add("container.cattle:" + container.isCattle());
        expectedTagsEvent.add("container.experimentStartTime:" + container.getExperimentStartTime());
        expectedTagsEvent.add("container.identity:" + container.getIdentity());
        expectedTagsEvent.add("chaosTime:" + chaosTime.getTime());
        expectedTagsEvent.add("experimentId:" + experimentId);
        expectedTagsEvent.add("experimentType:" + ExperimentType.STATE);
        expectedTagsEvent.add("experimentMethod:" + experimentMethod);
        expectedTagsEvent.add("notificationLevel:" + level.name());
        expectedTagsEvent.sort(Comparator.naturalOrder());
        chaosMessage = ChaosMessage.builder()
                                   .withMessage(message)
                                   .withTitle(title)
                                   .withNotificationLevel(level)
                                   .build();
        expectedTagsMessage.add("notificationLevel:" + level);
    }
    @Test
    public void logEvent () {
        StatsDClient client = Mockito.mock(StatsDClient.class);
        DataDogNotification notif = new DataDogNotification(client);
        notif.logNotification(chaosExperimentEvent);
        ArgumentCaptor<String> tagsCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
        Event expectedEvent = Event.builder().withAggregationKey(experimentId)
                                   .withAlertType(Event.AlertType.WARNING)
                                   .withTitle(chaosExperimentEvent.getTitle())
                                   .withText(chaosExperimentEvent.getMessage())
                                   .withSourceTypeName(DataDogNotification.DataDogEvent.SOURCE_TYPE)
                                   .build();
        verify(client, times(1)).recordEvent(eventCaptor.capture(), tagsCaptor.capture());
        List<String> actualTags = tagsCaptor.getAllValues();
        actualTags.sort(Comparator.naturalOrder());
        assertThat(actualTags, is(expectedTagsEvent));
        Event actualEvent = eventCaptor.getValue();
        assertEquals(actualEvent.getAggregationKey(), expectedEvent.getAggregationKey());
        assertEquals(actualEvent.getAlertType(), expectedEvent.getAlertType());
        assertEquals(actualEvent.getTitle(), expectedEvent.getTitle());
        assertEquals(actualEvent.getText(), expectedEvent.getText());
        assertEquals(actualEvent.getSourceTypeName(), expectedEvent.getSourceTypeName());
    }

    @Test
    public void logMessage () {
        StatsDClient client = Mockito.mock(StatsDClient.class);
        DataDogNotification notif = new DataDogNotification(client);
        notif.logNotification(chaosMessage);
        ArgumentCaptor<String> tagsCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
        Event expectedEvent = Event.builder()
                                   .withAlertType(Event.AlertType.WARNING)
                                   .withTitle(chaosMessage.getTitle())
                                   .withText(chaosMessage.getMessage())
                                   .withSourceTypeName(DataDogNotification.DataDogEvent.SOURCE_TYPE)
                                   .build();
        verify(client, times(1)).recordEvent(eventCaptor.capture(), tagsCaptor.capture());
        List<String> actualTags = tagsCaptor.getAllValues();
        actualTags.sort(Comparator.naturalOrder());
        assertThat(actualTags, is(expectedTagsMessage));
        Event actualEvent = eventCaptor.getValue();
        assertEquals(actualEvent.getAggregationKey(), expectedEvent.getAggregationKey());
        assertEquals(actualEvent.getAlertType(), expectedEvent.getAlertType());
        assertEquals(actualEvent.getTitle(), expectedEvent.getTitle());
        assertEquals(actualEvent.getText(), expectedEvent.getText());
        assertEquals(actualEvent.getSourceTypeName(), expectedEvent.getSourceTypeName());
    }

    @Test
    public void logEventFailure(){
        StatsDClient client = Mockito.mock(StatsDClient.class);
        doThrow(StatsDClientException.class).when(client).recordEvent(ArgumentMatchers.any(), ArgumentMatchers.any());
        DataDogNotification notif = new DataDogNotification(client);
        notif.logNotification(chaosExperimentEvent);
    }

    @Test
    public void mapLevel(){
        assertEquals(Event.AlertType.WARNING,dataDogEvent.mapLevel(NotificationLevel.WARN));
        assertEquals(Event.AlertType.ERROR,dataDogEvent.mapLevel(NotificationLevel.ERROR));
        assertEquals(Event.AlertType.SUCCESS,dataDogEvent.mapLevel(NotificationLevel.GOOD));
    }

    @Test
    public void buildMessage () {
        Event expectedEvent = Event.builder()
                                   .withText(message).withTitle(ChaosMessage.CHAOS_MESSAGE_PREFIX + " - " + title)
                                   .withAlertType(Event.AlertType.WARNING)
                                   .withSourceTypeName(DataDogNotification.DataDogEvent.SOURCE_TYPE)
                                   .build();
        Event actualEvent = dataDogEvent.buildFromNotification(chaosMessage);
        assertEquals(expectedEvent.getTitle(), actualEvent.getTitle());
        assertEquals(expectedEvent.getText(), actualEvent.getText());
        assertEquals(expectedEvent.getAlertType(), actualEvent.getAlertType());
        assertEquals(expectedEvent.getSourceTypeName(), actualEvent.getSourceTypeName());
    }

    @Test
    public void getTags () {
        StatsDClient client = Mockito.mock(StatsDClient.class);
        DataDogNotification notif = new DataDogNotification(client);
        notif.logNotification(chaosExperimentEvent);
        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(client, times(1)).recordEvent(ArgumentMatchers.any(), captor.capture());
        List<String> actualTags = captor.getAllValues();
        actualTags.sort(Comparator.naturalOrder());
        assertThat(actualTags, is(expectedTagsEvent));
    }
}