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

package com.thales.chaos.notification.services;

import org.jivesoftware.smack.tcp.XMPPTCPConnectionConfiguration;
import org.junit.Test;
import org.jxmpp.jid.impl.JidCreate;
import org.jxmpp.stringprep.XmppStringprepException;

import javax.net.ssl.SSLContext;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItems;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

public class XMPPNotificationServiceTest {
    private static final String USER_1 = "user1@localhost";
    private static final String USER_2 = "user2@localhost";
    private static final String ROOM_1 = "room@conference.localhost";
    private static final String ROOM_2 = "room2@conference.localhost";

    @Test
    public void createAddressBook () throws Exception {
        String recipients = USER_1 + "," + USER_2;
        String conferenceRooms = ROOM_1 + "," + ROOM_2;
        XMPPNotificationService service = new XMPPNotificationService();
        service.setRecipients(recipients);
        service.setConferenceRooms(conferenceRooms);
        XMPPNotificationService.AddressBook book = service.getAddressBook();
        assertEquals(2, book.getConferenceRooms().size());
        assertEquals(2, book.getRecipients().size());
        assertThat(book.getRecipients(), hasItems(JidCreate.entityBareFrom(USER_1), JidCreate.entityBareFrom(USER_2)));
        assertThat(book.getConferenceRooms(),
                hasItems(JidCreate.entityBareFrom(ROOM_1), JidCreate.entityBareFrom(ROOM_2)));
    }

    @Test
    public void createAddressBookUsersOnly () throws Exception {
        String recipients = USER_1 + "," + USER_2;
        XMPPNotificationService service = new XMPPNotificationService();
        service.setRecipients(recipients);
        XMPPNotificationService.AddressBook book = service.getAddressBook();
        assertEquals(0, book.getConferenceRooms().size());
        assertEquals(2, book.getRecipients().size());
        assertThat(book.getRecipients(), hasItems(JidCreate.entityBareFrom(USER_1), JidCreate.entityBareFrom(USER_2)));
    }

    @Test
    public void createAddressBookRoomsOnly () throws Exception {
        String conferenceRooms = ROOM_1;
        XMPPNotificationService service = new XMPPNotificationService();
        service.setConferenceRooms(conferenceRooms);
        XMPPNotificationService.AddressBook book = service.getAddressBook();
        assertEquals(1, book.getConferenceRooms().size());
        assertEquals(0, book.getRecipients().size());
        assertThat(book.getConferenceRooms(), hasItems(JidCreate.entityBareFrom(ROOM_1)));
        service.setRecipients("");
        book = service.getAddressBook();
        assertEquals(1, book.getConferenceRooms().size());
        assertEquals(0, book.getRecipients().size());
        assertThat(book.getConferenceRooms(), hasItems(JidCreate.entityBareFrom(ROOM_1)));
    }

    @Test
    public void getSecurityContext () throws Exception {
        String certFingerPrint = "CERTSHA256:F9:16:59:0B:93:72:66:A4:9A:DB:DF:2A:7F:8B:A3:CF:44:2B:A2:31:A8:1A:72:F5:7D:43:76:21:C6:2C:B3:81";
        XMPPNotificationService service = new XMPPNotificationService();
        assertEquals(SSLContext.getDefault(), service.getSecurityContext());
        service.setServerCertFingerprint("");
        assertEquals(SSLContext.getDefault(), service.getSecurityContext());
        service.setServerCertFingerprint(certFingerPrint);
        assertNotEquals(SSLContext.getDefault(), service.getSecurityContext());
    }

    @Test
    public void getConfig () throws XmppStringprepException, NoSuchAlgorithmException, KeyManagementException {
        String user = "user";
        String password = "passwd";
        String domain = "domain";
        String hostname = "hostname";
        XMPPTCPConnectionConfiguration connectionConfigurationExpected = XMPPTCPConnectionConfiguration.builder()
                                                                                                       .setUsernameAndPassword(
                                                                                                               user,
                                                                                                               password)
                                                                                                       .setXmppDomain(
                                                                                                               domain)
                                                                                                       .setHost(hostname)
                                                                                                       .setCustomSSLContext(
                                                                                                               SSLContext
                                                                                                                       .getDefault())
                                                                                                       .build();
        XMPPNotificationService service = new XMPPNotificationService();
        service.setUser(user);
        service.setPassword(password);
        service.setDomain(domain);
        service.setHostname(hostname);
        XMPPTCPConnectionConfiguration connectionConfiguration = service.getConfiguration().get();
        assertEquals(connectionConfigurationExpected.getUsername(), connectionConfiguration.getUsername());
        assertEquals(connectionConfigurationExpected.getPassword(), connectionConfiguration.getPassword());
        assertEquals(connectionConfigurationExpected.getXMPPServiceDomain(),
                connectionConfiguration.getXMPPServiceDomain());
        assertEquals(connectionConfigurationExpected.getCustomSSLContext(),
                connectionConfiguration.getCustomSSLContext());
    }
}