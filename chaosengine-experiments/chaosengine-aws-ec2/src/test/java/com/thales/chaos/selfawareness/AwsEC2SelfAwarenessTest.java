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

package com.thales.chaos.selfawareness;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.UUID;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.*;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration
public class AwsEC2SelfAwarenessTest {
    private static final String RESPONSE = UUID.randomUUID().toString();
    @Autowired
    private AwsEC2SelfAwareness awsEC2SelfAwareness = new AwsEC2SelfAwareness();

    @Test
    public void isMe () {
        doReturn(RESPONSE).when(awsEC2SelfAwareness).fetchInstanceId();
        assertTrue(awsEC2SelfAwareness.isMe(RESPONSE));
        assertTrue(awsEC2SelfAwareness.isMe(RESPONSE)); // Assertion repeated for testing caching of the ID.
        assertFalse(awsEC2SelfAwareness.isMe(RESPONSE.substring(RESPONSE.length() - 1)));
        verify(awsEC2SelfAwareness, times(1)).fetchInstanceId();
    }

    @Configuration
    static class TestConfig {
        @Bean
        AwsEC2SelfAwareness awsEC2SelfAwareness () {
            return Mockito.spy(new AwsEC2SelfAwareness());
        }
    }
}