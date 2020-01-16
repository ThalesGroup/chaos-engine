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

package com.thales.chaos.platform;

import com.thales.chaos.container.Container;
import com.thales.chaos.platform.enums.ApiStatus;
import com.thales.chaos.platform.enums.PlatformHealth;
import com.thales.chaos.platform.enums.PlatformLevel;
import org.hamcrest.collection.IsEmptyCollection;
import org.hamcrest.collection.IsIterableContainingInAnyOrder;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.Collections;
import java.util.List;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class PlatformTest {
    @Mock
    private Container container;
    @Spy
    private Platform platform;

    @Before
    public void setUp () {
        platform = Mockito.spy(new Platform() {
            @Override
            public ApiStatus getApiStatus () {
                return null;
            }

            @Override
            public PlatformLevel getPlatformLevel () {
                return null;
            }

            @Override
            public PlatformHealth getPlatformHealth () {
                return null;
            }

            @Override
            public List<Container> generateRoster () {
                return Collections.singletonList(container);
            }

            @Override
            public boolean isContainerRecycled (Container container) {
                return false;
            }
        });
    }

    @Test
    public void getRoster () {
        platform.getRoster();
        platform.getRoster();
        verify(platform, times(1)).generateRoster();
        assertThat(platform.getRoster(), IsIterableContainingInAnyOrder.containsInAnyOrder(container));
    }

    @Test
    public void expireCachedRoster () {
        platform.getRoster();
        platform.expireCachedRoster();
        platform.getRoster();
        verify(platform, times(2)).generateRoster();
        assertThat(platform.getRoster(), IsIterableContainingInAnyOrder.containsInAnyOrder(container));
    }

    @Test
    public void getRosterByAggregationIdentifier () {
        Container container1 = mock(Container.class);
        Container container2 = mock(Container.class);
        Container container3 = mock(Container.class);
        Container container4 = mock(Container.class);
        doReturn("aggregateOne").when(container1).getAggregationIdentifier();
        doReturn("aggregateOne").when(container2).getAggregationIdentifier();
        doReturn("aggregateTwo").when(container3).getAggregationIdentifier();
        doReturn("aggregateTwo").when(container4).getAggregationIdentifier();
        doReturn(List.of(container1, container2, container3, container4)).when(platform).getRoster();
        assertThat(platform.getRosterByAggregationId("aggregateOne"), containsInAnyOrder(container1, container2));
        assertThat(platform.getRosterByAggregationId("aggregateTwo"), containsInAnyOrder(container3, container4));
        assertThat(platform.getRosterByAggregationId("aggregateThree"), IsEmptyCollection.empty());
    }
}