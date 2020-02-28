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

import org.junit.Test;
import org.jxmpp.jid.impl.JidCreate;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItems;
import static org.junit.Assert.assertEquals;

public class XMPPNotificationServiceTest {
    private static final String USER_1 = "user1@localhost";
    private static final String USER_2 = "user2@localhost";
    private static final String ROOM_1 = "room@conference.localhost";
    private static final String ROOM_2 = "room2@conference.localhost";

    @Test
    public void createAddressBook () throws Exception {
        String recipients = USER_1 + "," + USER_2;
        String conferenceRooms = ROOM_1 + "," + ROOM_2;
        XMPPNotificationService.AddressBook book = new XMPPNotificationService.AddressBook(recipients, conferenceRooms);
        assertEquals(2, book.getConferenceRooms().size());
        assertEquals(2, book.getRecipients().size());
        assertThat(book.getRecipients(), hasItems(JidCreate.entityBareFrom(USER_1), JidCreate.entityBareFrom(USER_2)));
        assertThat(book.getConferenceRooms(),
                hasItems(JidCreate.entityBareFrom(ROOM_1), JidCreate.entityBareFrom(ROOM_2)));
    }

    @Test
    public void createAddressBookUsersOnly () throws Exception {
        String recipients = USER_1 + "," + USER_2;
        XMPPNotificationService.AddressBook book = new XMPPNotificationService.AddressBook(recipients, null);
        assertEquals(0, book.getConferenceRooms().size());
        assertEquals(2, book.getRecipients().size());
        assertThat(book.getRecipients(), hasItems(JidCreate.entityBareFrom(USER_1), JidCreate.entityBareFrom(USER_2)));
    }

    @Test
    public void createAddressBookRoomsOnly () throws Exception {
        String conferenceRooms = ROOM_1;
        XMPPNotificationService.AddressBook book = new XMPPNotificationService.AddressBook(null, conferenceRooms);
        assertEquals(1, book.getConferenceRooms().size());
        assertEquals(0, book.getRecipients().size());
        assertThat(book.getConferenceRooms(), hasItems(JidCreate.entityBareFrom(ROOM_1)));
    }
}