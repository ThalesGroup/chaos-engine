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

package com.thales.chaos.refresh.impl;

import com.thales.chaos.refresh.RefreshManager;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.cloud.endpoint.RefreshEndpoint;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.Collection;
import java.util.Set;

import static org.hamcrest.collection.IsIterableContainingInAnyOrder.containsInAnyOrder;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.*;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration
public class ChaosRefreshManagerTest {
    @MockBean
    private RefreshEndpoint refreshEndpoint;
    @Autowired
    private RefreshManager refreshManager;

    @Test
    public void doRefresh () {
        doReturn(Set.of("beans", "more beans")).when(refreshEndpoint).refresh();
        Collection<String> strings = refreshManager.doRefresh();
        assertThat(strings, containsInAnyOrder("beans", "more beans"));
        verify(refreshEndpoint, atLeastOnce()).refresh();
    }

    @Configuration
    static class ContextConfiguration {
        @Autowired
        private RefreshEndpoint refreshEndpoint;

        @Bean
        public RefreshManager refreshManager () {
            return Mockito.spy(new ChaosRefreshManager());
        }
    }
}