/*
 *    Copyright (c) 2018 - 2020, Thales DIS CPL Canada, Inc
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

package com.thales.chaos.notification.impl;

import com.thales.chaos.container.Container;
import com.thales.chaos.container.annotations.Identifier;
import com.thales.chaos.container.enums.ContainerHealth;
import com.thales.chaos.experiment.enums.ExperimentType;
import com.thales.chaos.notification.ChaosNotification;
import com.thales.chaos.notification.datadog.DataDogIdentifier;
import com.thales.chaos.notification.enums.NotificationLevel;
import com.thales.chaos.notification.message.ChaosExperimentEvent;
import com.thales.chaos.notification.message.ChaosMessage;
import com.thales.chaos.notification.services.XMPPNotificationService;
import com.thales.chaos.platform.Platform;
import com.thales.chaos.util.HttpUtils;
import org.hamcrest.Matchers;
import org.jivesoftware.smack.AbstractXMPPConnection;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smackx.muc.MultiUserChat;
import org.jivesoftware.smackx.xhtmlim.XHTMLManager;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.jxmpp.jid.EntityBareJid;
import org.jxmpp.jid.impl.JidCreate;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;

import javax.validation.constraints.NotNull;
import java.io.IOException;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import static java.util.UUID.randomUUID;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class XMPPNotificationTest {
    private static final String USER_1 = "user1@localhost";
    private static final String USER_2 = "user2@localhost";
    private static final String ROOM_1 = "room@conference.localhost";
    private static final String ROOM_2 = "room2@conference.localhost";
    private static final String TITLE = "Title";
    private static final String MESSAGE = "Lorem Ipsum";
    Container dummyContainer = new Container() {
        @Identifier
        private String name = "dummyContainer";

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
            return name;
        }

        @Override
        public String getAggregationIdentifier () {
            return getSimpleName();
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
    @Spy
    private XMPPNotification xmppNotification = new XMPPNotification();

    @Test
    public void mapLevelToColor () {
        assertEquals(XMPPNotification.ParagraphStyle.NOTIFICATION_MESSAGE_ERROR,
                xmppNotification.mapLevelToStyle(NotificationLevel.ERROR));
        assertEquals(XMPPNotification.ParagraphStyle.NOTIFICATION_MESSAGE_WARN,
                xmppNotification.mapLevelToStyle(NotificationLevel.WARN));
        assertEquals(XMPPNotification.ParagraphStyle.NOTIFICATION_MESSAGE_GOOD,
                xmppNotification.mapLevelToStyle(NotificationLevel.GOOD));
    }

    @Test
    public void sendDirectMessage () throws Exception {
        AbstractXMPPConnection abstractXMPPConnection = Mockito.mock(AbstractXMPPConnection.class);
        Message msg = new Message();
        doReturn(abstractXMPPConnection).when(xmppNotification).getConnection();
        xmppNotification.sendDirectMessage(msg, JidCreate.entityBareFrom(USER_1));
        assertEquals(Message.Type.normal, msg.getType());
        verify(abstractXMPPConnection, times(1)).disconnect();
    }

    @Test
    public void getMultiUserChat () throws Exception {
        EntityBareJid roomName = JidCreate.entityBareFrom(USER_1);
        AbstractXMPPConnection abstractXMPPConnection = Mockito.mock(AbstractXMPPConnection.class);
        MultiUserChat multiUserChat = xmppNotification.getMultiUserChat(abstractXMPPConnection, roomName);
        assertThat(roomName, equalTo(multiUserChat.getRoom()));
    }

    @Test
    public void sendMultiUserMessage () throws Exception {
        Message msg = new Message();
        EntityBareJid roomName = JidCreate.entityBareFrom(USER_1);
        AbstractXMPPConnection abstractXMPPConnection = Mockito.mock(AbstractXMPPConnection.class);
        doReturn(abstractXMPPConnection).when(xmppNotification).getConnection();
        MultiUserChat multiUserChat = Mockito.mock(MultiUserChat.class);
        doReturn(multiUserChat).when(xmppNotification).getMultiUserChat(abstractXMPPConnection, roomName);
        xmppNotification.sendMultiUserMessage(msg, JidCreate.entityBareFrom(roomName));
        assertEquals(Message.Type.groupchat, msg.getType());
        verify(abstractXMPPConnection, times(1)).disconnect();
    }

    @Test
    public void buildMessageChaosMessage () {
        ChaosNotification messageNotif = ChaosMessage.builder().withTitle(TITLE).withMessage(MESSAGE)
                                                     .withNotificationLevel(NotificationLevel.GOOD)
                                                     .build();
        Message notification = xmppNotification.buildMessage(messageNotif);
        assertEquals(messageNotif.getTitle(), notification.getSubject());
        assertEquals(messageNotif.getMessage(), notification.getBody());
        assertTrue(XHTMLManager.isXHTMLMessage(notification));
        List<CharSequence> bodies = XHTMLManager.getBodies(notification);
        assertEquals(5, bodies.size());
        String bodyTitle = String.valueOf(bodies.get(0));
        assertThat(bodyTitle,
                allOf(Matchers.containsString(messageNotif.getTitle()),
                        Matchers.containsString(XMPPNotification.SIZE_NORMAL),
                        Matchers.containsString(XMPPNotification.WEIGHT_NORMAL),
                        Matchers.containsString(XMPPNotification.COLOR_BLUE)));
        String bodyMessageHeader = String.valueOf(bodies.get(1));
        assertThat(bodyMessageHeader,
                allOf(Matchers.containsString(XMPPNotification.MESSAGE_HEADER),
                        Matchers.containsString(XMPPNotification.SIZE_NORMAL),
                        Matchers.containsString(XMPPNotification.WEIGHT_BOLD),
                        Matchers.containsString(XMPPNotification.COLOR_NORMAL)));
        String bodyMessage = String.valueOf(bodies.get(2));
        assertThat(bodyMessage,
                allOf(Matchers.containsString(messageNotif.getMessage()),
                        Matchers.containsString(XMPPNotification.SIZE_SMALLER),
                        Matchers.containsString(XMPPNotification.WEIGHT_NORMAL),
                        Matchers.containsString(XMPPNotification.COLOR_GREEN)));
        String instanceHeader = String.valueOf(bodies.get(3));
        assertThat(instanceHeader,
                allOf(Matchers.containsString(XMPPNotification.INSTANCE_HEADER),
                        Matchers.containsString(XMPPNotification.SIZE_NORMAL),
                        Matchers.containsString(XMPPNotification.WEIGHT_BOLD),
                        Matchers.containsString(XMPPNotification.COLOR_NORMAL)));
        String instance = String.valueOf(bodies.get(4));
        assertThat(instance,
                allOf(Matchers.containsString(HttpUtils.getMachineHostname()),
                        Matchers.containsString(XMPPNotification.SIZE_SMALLER),
                        Matchers.containsString(XMPPNotification.WEIGHT_NORMAL),
                        Matchers.containsString(XMPPNotification.COLOR_NORMAL)));
    }

    @Test
    public void buildMessageEventMessage () {
        Date date = Date.from(Instant.now());
        String experimentId = randomUUID().toString();
        String experimentMethod = "Restart Container";
        ExperimentType experimentType = ExperimentType.STATE;
        ChaosNotification eventNotif = ChaosExperimentEvent.builder()
                                                           .withChaosTime(date)
                                                           .withTitle(TITLE)
                                                           .withMessage(MESSAGE)
                                                           .withTargetContainer(dummyContainer)
                                                           .withExperimentMethod(experimentMethod)
                                                           .withExperimentType(experimentType)
                                                           .withNotificationLevel(NotificationLevel.ERROR)
                                                           .withExperimentId(experimentId)
                                                           .build();
        Message notification = xmppNotification.buildMessage(eventNotif);
        assertEquals(eventNotif.getTitle(), notification.getSubject());
        assertEquals(eventNotif.getMessage(), notification.getBody());
        assertTrue(XHTMLManager.isXHTMLMessage(notification));
        List<CharSequence> bodies = XHTMLManager.getBodies(notification);
        assertEquals(19, bodies.size());
        String bodyTitle = String.valueOf(bodies.get(0));
        assertThat(bodyTitle,
                allOf(Matchers.containsString(eventNotif.getTitle()),
                        Matchers.containsString(XMPPNotification.SIZE_NORMAL),
                        Matchers.containsString(XMPPNotification.WEIGHT_NORMAL),
                        Matchers.containsString(XMPPNotification.COLOR_BLUE)));
        String bodyMessageHeader = String.valueOf(bodies.get(1));
        assertThat(bodyMessageHeader,
                allOf(Matchers.containsString(XMPPNotification.MESSAGE_HEADER),
                        Matchers.containsString(XMPPNotification.SIZE_NORMAL),
                        Matchers.containsString(XMPPNotification.WEIGHT_BOLD),
                        Matchers.containsString(XMPPNotification.COLOR_NORMAL)));
        String bodyMessage = String.valueOf(bodies.get(2));
        assertThat(bodyMessage,
                allOf(Matchers.containsString(eventNotif.getMessage()),
                        Matchers.containsString(XMPPNotification.SIZE_SMALLER),
                        Matchers.containsString(XMPPNotification.WEIGHT_NORMAL),
                        Matchers.containsString(XMPPNotification.COLOR_RED)));
        String instanceHeader = String.valueOf(bodies.get(3));
        assertThat(instanceHeader,
                allOf(Matchers.containsString(XMPPNotification.INSTANCE_HEADER),
                        Matchers.containsString(XMPPNotification.SIZE_NORMAL),
                        Matchers.containsString(XMPPNotification.WEIGHT_BOLD),
                        Matchers.containsString(XMPPNotification.COLOR_NORMAL)));
        String instance = String.valueOf(bodies.get(4));
        assertThat(instance,
                allOf(Matchers.containsString(HttpUtils.getMachineHostname()),
                        Matchers.containsString(XMPPNotification.SIZE_SMALLER),
                        Matchers.containsString(XMPPNotification.WEIGHT_NORMAL),
                        Matchers.containsString(XMPPNotification.COLOR_NORMAL)));
        List<String> bodyList = bodies.stream().map(String::valueOf).collect(Collectors.toList());
        List<String> expectedHeaders = List.of("Container Type",
                "Simple Name",
                "Experiment Id",
                "Experiment Method",
                "Experiment Type",
                "Aggregation Identifier",
                "Timestamp");
        for (String field : expectedHeaders) {
            assertThat(bodyList,
                    hasItem(allOf(Matchers.containsString(field),
                            Matchers.containsString(XMPPNotification.SIZE_NORMAL),
                            Matchers.containsString(XMPPNotification.WEIGHT_BOLD),
                            Matchers.containsString(XMPPNotification.COLOR_NORMAL))));
        }
        List<String> expectedFields = List.of(experimentId,
                experimentMethod,
                experimentType.toString(),
                date.toInstant().toString());
        for (String field : expectedFields) {
            assertThat(bodyList,
                    hasItem(allOf(Matchers.containsString(field),
                            Matchers.containsString(XMPPNotification.SIZE_SMALLER),
                            Matchers.containsString(XMPPNotification.WEIGHT_NORMAL),
                            Matchers.containsString(XMPPNotification.COLOR_NORMAL))));
        }
    }

    @Test
    public void logNotification () throws Exception {
        ChaosNotification messageNotif = ChaosMessage.builder()
                                                     .withTitle(TITLE)
                                                     .withMessage(MESSAGE)
                                                     .withNotificationLevel(NotificationLevel.GOOD)
                                                     .build();
        String recipients = USER_1 + "," + USER_2;
        String conferenceRooms = ROOM_1 + "," + ROOM_2;
        XMPPNotificationService.AddressBook book = new XMPPNotificationService.AddressBook(recipients, conferenceRooms);
        AbstractXMPPConnection abstractXMPPConnection = Mockito.mock(AbstractXMPPConnection.class);
        doReturn(abstractXMPPConnection).when(xmppNotification).getConnection();
        MultiUserChat multiUserChat = Mockito.mock(MultiUserChat.class);
        doReturn(multiUserChat).when(xmppNotification).getMultiUserChat(any(), any());
        xmppNotification.setAddressBook(book);
        xmppNotification.logNotification(messageNotif);
        verify(xmppNotification, times(2)).sendDirectMessage(any(), any());
        verify(xmppNotification, times(2)).sendMultiUserMessage(any(), any());
        verify(xmppNotification, times(4)).getConnection();
    }

    @Test
    public void logNotificationError () throws Exception {
        ChaosNotification messageNotif = ChaosMessage.builder()
                                                     .withTitle(TITLE)
                                                     .withMessage(MESSAGE)
                                                     .withNotificationLevel(NotificationLevel.GOOD)
                                                     .build();
        String recipients = USER_1 + "," + USER_2;
        String conferenceRooms = ROOM_1 + "," + ROOM_2;
        XMPPNotificationService.AddressBook book = new XMPPNotificationService.AddressBook(recipients, conferenceRooms);
        xmppNotification.setAddressBook(book);
        doThrow(IOException.class).when(xmppNotification).getConnection();
        xmppNotification.logNotification(messageNotif);
        verify(xmppNotification, times(2)).sendDirectMessage(any(), any());
        verify(xmppNotification, times(2)).sendMultiUserMessage(any(), any());
        verify(xmppNotification, times(4)).getConnection();
    }
}