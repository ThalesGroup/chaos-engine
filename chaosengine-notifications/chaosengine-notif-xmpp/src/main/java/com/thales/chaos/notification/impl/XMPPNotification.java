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
    static final String EVENT_TIMESTAMP_HEADER = "Timestamp";
    private final Logger log = LoggerFactory.getLogger(getClass());
    private final List<String> knownChaosEventFields = List.of("experimentId",
            "experimentType",
            "experimentMethod",
            "chaosTime",
            "targetContainer");
    private final Collection<String> knownContainerFields = Set.of("aggregationIdentifier",
            "simpleName",
            "containerType");
    private final Collection<String> skipFields = Set.of("message", "title", "notificationLevel");

    Message buildMessage (ChaosNotification notification) {
        Message msg = new Message();
        msg.setSubject(notification.getTitle());
        msg.setBody(notification.getMessage());
        XHTMLManager.addBody(msg, composeParagraph(notification.getTitle(), ParagraphStyle.NOTIFICATION_TITLE));
        XHTMLManager.addBody(msg, composeParagraph(MESSAGE_HEADER, ParagraphStyle.NOTIFICATION_MESSAGE_HEADER));
        XHTMLManager.addBody(msg,
                composeParagraph(notification.getMessage(), mapLevelToStyle(notification.getNotificationLevel())));
        XHTMLManager.addBody(msg, composeParagraph(INSTANCE_HEADER, ParagraphStyle.NOTIFICATION_FIELD_HEADER));
        XHTMLManager.addBody(msg, composeParagraph(HttpUtils.getMachineHostname(), ParagraphStyle.NOTIFICATION_FIELD));
        collectExperimentEventFields(notification.asMap(), msg);
        return msg;
    }

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

    XHTMLText composeParagraph (String text, ParagraphStyle style) {
        XHTMLText paragraph = new XHTMLText("", "");
        paragraph.appendBrTag();
        paragraph.appendOpenParagraphTag(style.getStyling());
        paragraph.append(text);
        paragraph.appendCloseParagraphTag();
        paragraph.appendCloseBodyTag();
        return paragraph;
    }

    ParagraphStyle mapLevelToStyle (NotificationLevel level) {
        switch (level) {
            case WARN:
                return ParagraphStyle.NOTIFICATION_MESSAGE_WARN;
            case ERROR:
                return ParagraphStyle.NOTIFICATION_MESSAGE_ERROR;
            default:
                return ParagraphStyle.NOTIFICATION_MESSAGE_GOOD;
        }
    }

    private void collectExperimentEventFields (Map<String, Object> fieldMap, Message msg) {
        fieldMap.entrySet()
                .stream()
                .filter(entry -> Objects.nonNull(entry.getValue()))
                .filter(entry -> !skipFields.contains(entry.getKey()))
                .sorted(sortMapByList(knownChaosEventFields))
                .forEach(field -> {
                    switch (field.getKey()) {
                        case "chaosTime":
                            Instant time = Instant.ofEpochMilli((Long) field.getValue());
                            XHTMLManager.addBody(msg,
                                    composeParagraph(EVENT_TIMESTAMP_HEADER, ParagraphStyle.NOTIFICATION_FIELD_HEADER));
                            XHTMLManager.addBody(msg,
                                    composeParagraph(time.toString(), ParagraphStyle.NOTIFICATION_FIELD));
                            break;
                        case "targetContainer":
                            Map<String, String> map = (Map<String, String>) field.getValue();
                            map.entrySet()
                               .stream()
                               .sorted(Map.Entry.comparingByKey())
                               .filter(entry -> knownContainerFields.contains(entry.getKey()))
                               .forEach(entry -> {
                                   XHTMLManager.addBody(msg,
                                           composeParagraph(StringUtils.convertCamelCaseToSentence(entry.getKey()),
                                                   ParagraphStyle.NOTIFICATION_FIELD_HEADER));
                                   XHTMLManager.addBody(msg,
                                           composeParagraph(entry.getValue(), ParagraphStyle.NOTIFICATION_FIELD));
                               });
                            break;
                        default:
                            XHTMLManager.addBody(msg,
                                    composeParagraph(StringUtils.convertCamelCaseToSentence(field.getKey()),
                                            ParagraphStyle.NOTIFICATION_FIELD_HEADER));
                            XHTMLManager.addBody(msg,
                                    composeParagraph(field.getValue().toString(), ParagraphStyle.NOTIFICATION_FIELD));
                    }
                });
    }

    private <T> Comparator<Map.Entry<T, ?>> sortMapByList (List<? super T> list) {
        return (e1, e2) -> {
            int i = indexOrHash(e1.getKey(), list);
            int j = indexOrHash(e2.getKey(), list);
            return Integer.compare(i, j);
        };
    }

    private <T> int indexOrHash (T element, List<? super T> list) {
        int index = list.indexOf(element);
        return index >= 0 ? index : Objects.hash(element) + list.size();
    }

    enum ParagraphStyle {
        NOTIFICATION_TITLE(SIZE_NORMAL + WEIGHT_NORMAL + COLOR_BLUE),
        NOTIFICATION_MESSAGE_HEADER(SIZE_NORMAL + WEIGHT_BOLD + COLOR_NORMAL),
        NOTIFICATION_MESSAGE_GOOD(SIZE_SMALLER + WEIGHT_NORMAL + COLOR_GREEN),
        NOTIFICATION_MESSAGE_WARN(SIZE_SMALLER + WEIGHT_NORMAL + COLOR_ORANGE),
        NOTIFICATION_MESSAGE_ERROR(SIZE_SMALLER + WEIGHT_NORMAL + COLOR_RED),
        NOTIFICATION_FIELD_HEADER(SIZE_NORMAL + WEIGHT_BOLD + COLOR_NORMAL),
        NOTIFICATION_FIELD(SIZE_SMALLER + WEIGHT_NORMAL + COLOR_NORMAL);
        private String styling;

        ParagraphStyle (String styling) {
            this.styling = styling;
        }

        public String getStyling () {
            return styling;
        }
    }

    void sendDirectNotifications (Message msg) {
        log.debug("List of recipients {}", addressBook.getRecipients());
        for (EntityBareJid jid : addressBook.getRecipients()) {
            try {
                log.info("Sending XMPP notification to: {}", jid);
                sendDirectMessage(msg, jid);
                log.debug("XMPP  notification to {} sent", jid);
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
