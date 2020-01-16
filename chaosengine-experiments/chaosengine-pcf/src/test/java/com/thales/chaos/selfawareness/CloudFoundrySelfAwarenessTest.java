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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class CloudFoundrySelfAwarenessTest {
    private static final String engineName = "THIS IS MY ENGINE";
    private static final List<String> linkedApplications = Arrays.asList("ChaosMongo", "ChaosUtil");
    @Autowired
    private CloudFoundrySelfAwareness cloudFoundrySelfAwareness;

    @Test
    public void isMe () {
        ReflectionTestUtils.setField(cloudFoundrySelfAwareness, "applicationName", engineName);
        assertTrue(cloudFoundrySelfAwareness.isMe(engineName));
        assertFalse(cloudFoundrySelfAwareness.isMe("TEST STRING"));
    }

    @Test
    public void isLinkedApplication () {
        ReflectionTestUtils.setField(cloudFoundrySelfAwareness, "linkedApplicationNames", linkedApplications);
        assertTrue(cloudFoundrySelfAwareness.isFriendly("ChaosMongo"));
        assertTrue(cloudFoundrySelfAwareness.isFriendly("ChaosUtil"));
        assertFalse(cloudFoundrySelfAwareness.isFriendly(engineName));
    }

    @Configuration
    static class ContextConfiguration {
        @Bean
        CloudFoundrySelfAwareness cloudFoundrySelfAwareness () {
            return CloudFoundrySelfAwareness.builder()
                                            .withApplicationName(engineName)
                                            .withLinkedApplicationNames(linkedApplications)
                                            .build();
        }
    }
}
