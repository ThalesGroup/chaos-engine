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

import com.thales.chaos.notification.ChaosNotification;
import com.thales.chaos.notification.NotificationMethods;
import com.thales.chaos.notification.enums.NotificationLevel;
import com.thales.chaos.notification.services.XMPPNotificationService;
import com.thales.chaos.util.HttpUtils;
import com.thales.chaos.util.StringUtils;
import org.jivesoftware.smack.AbstractXMPPConnection;
import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.chat2.Chat;
import org.jivesoftware.smack.chat2.ChatManager;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.tcp.XMPPTCPConnection;
import org.jivesoftware.smack.tcp.XMPPTCPConnectionConfiguration;
import org.jivesoftware.smackx.muc.MultiUserChat;
import org.jivesoftware.smackx.muc.MultiUserChatManager;
import org.jivesoftware.smackx.xhtmlim.XHTMLManager;
import org.jivesoftware.smackx.xhtmlim.XHTMLText;
import org.jxmpp.jid.EntityBareJid;
import org.jxmpp.jid.impl.JidCreate;
import org.jxmpp.jid.parts.Resourcepart;
import org.jxmpp.stringprep.XmppStringprepException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Instant;
import java.util.*;

@Component
@ConditionalOnProperty(name = "xmpp.enabled", havingValue = "true")
public class XMPPNotification implements NotificationMethods {
    static final String SIZE_NORMAL = "font-size:normal;";
    static final String SIZE_SMALLER = "font-size:smaller;";
    static final String WEIGHT_NORMAL = "font-weight: normal;";
    static final String WEIGHT_BOLD = "font-weight: bold;";
    static final String COLOR_RED = "color: red;";
    static final String COLOR_BLUE = "color: blue;";
    static final String COLOR_GREEN = "color: green;";
    static final String COLOR_ORANGE = "color: orange;";
    static final String COLOR_NORMAL = "color: initial;";
    static final String MESSAGE_HEADER = "Message";
    static final String INSTANCE_HEADER = "Chaos Engine Instance";
    static final String EVENT_TIMESTEMP_HEADER = "Timestamp";
    private final Logger log = LoggerFactory.getLogger(getClass());
    private final Collection<String> knownChaosEventFields = List.of("title",
            "message",
            "notificationLevel",
            "experimentId",
            "experimentType",
            "experimentMethod",
            "chaosTime",
            "targetContainer");
    private final Collection<String> knownContainerFields = List.of("aggregationIdentifier",
            "simpleName",
            "containerType");
    @Autowired
    private XMPPTCPConnectionConfiguration configuration;
    @Autowired
    private XMPPNotificationService.AddressBook addressBook;

    @Autowired
    public XMPPNotification () {
        log.info("XMPP notification channel created");
    }

    void setAddressBook (XMPPNotificationService.AddressBook addressBook) {
        this.addressBook = addressBook;
    }

    AbstractXMPPConnection getConnection () throws InterruptedException, XMPPException, SmackException, IOException {
        AbstractXMPPConnection connection = new XMPPTCPConnection(configuration);
        connection.connect();
        connection.login();
        return connection;
    }

    Chat getSimpleUserChat (AbstractXMPPConnection connection, EntityBareJid jid) {
        ChatManager chatManager = ChatManager.getInstanceFor(connection);
        chatManager.setXhmtlImEnabled(true);
        return chatManager.chatWith(jid);
    }

    void sendDirectMessage (Message msg,
                            EntityBareJid jid) throws InterruptedException, IOException, SmackException, XMPPException {
        AbstractXMPPConnection connection = getConnection();
        Chat chat = getSimpleUserChat(connection, jid);
        msg.setType(Message.Type.normal);
        chat.send(msg);
        connection.disconnect();
    }

    MultiUserChat getMultiUserChat (AbstractXMPPConnection connection,
                                    EntityBareJid jid) throws XmppStringprepException {
        MultiUserChatManager manager = MultiUserChatManager.getInstanceFor(connection);
        return manager.getMultiUserChat(JidCreate.entityBareFrom(jid));
    }

    void sendMultiUserMessage (Message msg,
                               EntityBareJid jid) throws InterruptedException, IOException, SmackException, XMPPException {
        AbstractXMPPConnection connection = getConnection();
        Resourcepart room = Resourcepart.from(jid.getLocalpart().asUnescapedString());
        MultiUserChat muc = getMultiUserChat(connection, jid);
        muc.join(room);
        msg.setType(Message.Type.groupchat);
        muc.sendMessage(msg);
        connection.disconnect();
    }

    String mapLevelToColor (NotificationLevel level) {
        switch (level) {
            case WARN:
                return COLOR_ORANGE;
            case ERROR:
                return COLOR_RED;
            default:
                return COLOR_GREEN;
        }
    }

    XHTMLText composeMessageTitle (String notificationTitle) {
        XHTMLText title = new XHTMLText("", "");
        title.appendBrTag();
        title.appendOpenParagraphTag(SIZE_NORMAL + WEIGHT_NORMAL + COLOR_BLUE);
        title.append(notificationTitle);
        title.appendCloseParagraphTag();
        title.appendCloseBodyTag();
        return title;
    }

    XHTMLText composeMessage (String notificationMessage, NotificationLevel level) {
        XHTMLText message = new XHTMLText("", "");
        message.appendBrTag();
        message.appendOpenParagraphTag(SIZE_SMALLER + WEIGHT_NORMAL + mapLevelToColor(level));
        message.append(notificationMessage);
        message.appendCloseParagraphTag();
        message.appendCloseBodyTag();
        return message;
    }

    XHTMLText composeParagraphHeader (String headerName) {
        XHTMLText paragraphHeader = new XHTMLText("", "");
        paragraphHeader.appendBrTag();
        paragraphHeader.appendOpenParagraphTag(SIZE_NORMAL + WEIGHT_BOLD + COLOR_NORMAL);
        paragraphHeader.append(headerName);
        paragraphHeader.appendCloseParagraphTag();
        paragraphHeader.appendCloseBodyTag();
        return paragraphHeader;
    }

    XHTMLText composeText (String notificationText) {
        XHTMLText text = new XHTMLText("", "");
        text.appendBrTag();
        text.appendOpenParagraphTag(SIZE_SMALLER + WEIGHT_NORMAL + COLOR_NORMAL);
        text.append(notificationText);
        text.appendCloseParagraphTag();
        text.appendCloseBodyTag();
        return text;
    }

    Message buildMessage (ChaosNotification notification) {
        Message msg = new Message();
        msg.setSubject(notification.getTitle());
        msg.setBody(notification.getMessage());
        XHTMLManager.addBody(msg, composeMessageTitle(notification.getTitle()));
        XHTMLManager.addBody(msg, composeParagraphHeader(MESSAGE_HEADER));
        XHTMLManager.addBody(msg, composeMessage(notification.getMessage(), notification.getNotificationLevel()));
        XHTMLManager.addBody(msg, composeParagraphHeader(INSTANCE_HEADER));
        XHTMLManager.addBody(msg, composeText(HttpUtils.getMachineHostname()));
        collectExperimentEventFields(notification.asMap(), msg);
        return msg;
    }

    private void collectExperimentEventFields (Map<String, Object> fieldMap, Message msg) {
        fieldMap.entrySet()
                .stream()
                .filter(e -> knownChaosEventFields.contains(e.getKey()))
                .filter(e -> e.getKey().startsWith("experiment") && e.getValue() != null)
                .forEach(e -> {
                    XHTMLManager.addBody(msg,
                            composeParagraphHeader(StringUtils.convertCamelCaseToSentence(e.getKey())));
                    XHTMLManager.addBody(msg, composeText(e.getValue().toString()));
                });
        Optional.ofNullable(fieldMap.get("chaosTime"))
                .filter(Long.class::isInstance)
                .map(Long.class::cast)
                .map(Instant::ofEpochMilli)
                .ifPresent(time -> {
                    XHTMLManager.addBody(msg, composeParagraphHeader(EVENT_TIMESTEMP_HEADER));
                    XHTMLManager.addBody(msg, composeText(time.toString()));
                });
        Optional.ofNullable(fieldMap.get("targetContainer"))
                .filter(Map.class::isInstance)
                .map(o -> (Map<String, String>) o)
                .orElse(Collections.emptyMap())
                .entrySet()
                .stream()
                .sorted(Comparator.comparing(Map.Entry::getKey))
                .filter(entry -> knownContainerFields.contains(entry.getKey()))
                .forEach(e -> {
                    XHTMLManager.addBody(msg,
                            composeParagraphHeader(StringUtils.convertCamelCaseToSentence(e.getKey())));
                    XHTMLManager.addBody(msg, composeText(e.getValue()));
                });
    }

    void sendDirectNotifications (Message msg) {
        log.debug("List of recipients {}", addressBook.getRecipients());
        for (EntityBareJid jid : addressBook.getRecipients()) {
            try {
                log.debug("Sending XMPP notification to: {}", jid);
                sendDirectMessage(msg, jid);
                log.debug("XMPP  notification to {} send", jid);
            } catch (Exception e) {
                log.error("Cannot send XMPP notification to: {}", jid, e);
            }
        }
    }

    void sendRoomNotifications (Message msg) {
        log.debug("List of conference rooms {}", addressBook.getConferenceRooms());
        for (EntityBareJid jid : addressBook.getConferenceRooms()) {
            try {
                log.debug("Sending XMPP notification to conference room: {}", jid);
                sendMultiUserMessage(msg, jid);
                log.debug("XMPP  notification to {} send", jid);
            } catch (Exception e) {
                log.error("Cannot send XMPP notification to conference room: {}", jid, e);
            }
        }
    }

    @Override
    public void logNotification (ChaosNotification notification) {
        Message msg = buildMessage(notification);
        sendDirectNotifications(msg);
        sendRoomNotifications(msg);
    }
}
