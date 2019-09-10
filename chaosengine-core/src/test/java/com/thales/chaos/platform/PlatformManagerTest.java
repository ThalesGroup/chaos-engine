/*
 *    Copyright (c) 2019 Thales Group
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

import com.thales.chaos.platform.enums.PlatformHealth;
import com.thales.chaos.platform.enums.PlatformLevel;
import org.hamcrest.Matchers;
import org.hamcrest.collection.IsIterableContainingInAnyOrder;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.time.Instant;
import java.util.Collection;
import java.util.Optional;
import java.util.Set;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration
public class PlatformManagerTest {
    @MockBean(name = "firstPlatform")
    private Platform platform1;
    @MockBean(name = "secondPlatform")
    private Platform platform2;
    private Collection<Platform> platformCollection;
    @Autowired
    private PlatformManager platformManager;

    @Before
    public void setUp () {
        platformCollection = Set.of(platform1, platform2);
        doReturn(true).when(platform1).canExperiment();
        doReturn(true).when(platform2).canExperiment();
        doReturn(true).when(platform1).hasEligibleContainersForExperiments();
        doReturn(true).when(platform2).hasEligibleContainersForExperiments();
        Instant now = Instant.now();
        doReturn(now).when(platform1).getNextChaosTime();
        doReturn(now.plusSeconds(1)).when(platform2).getNextChaosTime();
    }

    @Test
    public void getPlatforms () {
        assertThat(platformManager.getPlatforms(), IsIterableContainingInAnyOrder.containsInAnyOrder(platform1, platform2));
    }

    @Test
    public void getPlatformsOfLevel () {
        when(platform1.getPlatformLevel()).thenReturn(PlatformLevel.IAAS);
        when(platform2.getPlatformLevel()).thenReturn(PlatformLevel.PAAS);
        Collection<Platform> iaasPlatforms = platformManager.getPlatformsOfLevel(PlatformLevel.IAAS);
        Collection<Platform> paasPlatforms = platformManager.getPlatformsOfLevel(PlatformLevel.PAAS);
        assertThat(paasPlatforms, not(hasItem(platform1)));
        assertThat(iaasPlatforms, not(hasItem(platform2)));
        assertThat(iaasPlatforms, IsIterableContainingInAnyOrder.containsInAnyOrder(platform1));
        assertThat(paasPlatforms, IsIterableContainingInAnyOrder.containsInAnyOrder(platform2));
    }

    @Test
    public void getOverallHealth () {
        // Two different healths
        when(platform1.getPlatformHealth()).thenReturn(PlatformHealth.OK);
        when(platform2.getPlatformHealth()).thenReturn(PlatformHealth.DEGRADED);
        Assert.assertEquals(PlatformHealth.DEGRADED, platformManager.getOverallHealth());
        // Two different healths in reverse order
        when(platform1.getPlatformHealth()).thenReturn(PlatformHealth.DEGRADED);
        when(platform2.getPlatformHealth()).thenReturn(PlatformHealth.OK);
        // A different set of two healths
        Assert.assertEquals(PlatformHealth.DEGRADED, platformManager.getOverallHealth());
        when(platform1.getPlatformHealth()).thenReturn(PlatformHealth.DEGRADED);
        when(platform2.getPlatformHealth()).thenReturn(PlatformHealth.FAILED);
        // A final set of two healths.
        Assert.assertEquals(PlatformHealth.FAILED, platformManager.getOverallHealth());
        when(platform1.getPlatformHealth()).thenReturn(PlatformHealth.FAILED);
        when(platform2.getPlatformHealth()).thenReturn(PlatformHealth.OK);
        Assert.assertEquals(PlatformHealth.FAILED, platformManager.getOverallHealth());
        // Two Equal healths
        when(platform1.getPlatformHealth()).thenReturn(PlatformHealth.FAILED);
        when(platform2.getPlatformHealth()).thenReturn(PlatformHealth.FAILED);
        Assert.assertEquals(PlatformHealth.FAILED, platformManager.getOverallHealth());
        when(platform1.getPlatformHealth()).thenReturn(PlatformHealth.OK);
        when(platform2.getPlatformHealth()).thenReturn(PlatformHealth.OK);
        Assert.assertEquals(PlatformHealth.OK, platformManager.getOverallHealth());
    }

    @Test
    public void getHealthOfPlatformLevel () {
        when(platform1.getPlatformLevel()).thenReturn(PlatformLevel.PAAS);
        when(platform1.getPlatformHealth()).thenReturn(PlatformHealth.DEGRADED);
        when(platform2.getPlatformLevel()).thenReturn(PlatformLevel.IAAS);
        when(platform2.getPlatformHealth()).thenReturn(PlatformHealth.FAILED);
        Assert.assertEquals(PlatformHealth.DEGRADED, platformManager.getHealthOfPlatformLevel(PlatformLevel.PAAS));
        Assert.assertEquals(PlatformHealth.FAILED, platformManager.getHealthOfPlatformLevel(PlatformLevel.IAAS));
    }

    @Test
    public void getPlatformLevels () {
        when(platform1.getPlatformLevel()).thenReturn(PlatformLevel.IAAS);
        when(platform2.getPlatformLevel()).thenReturn(PlatformLevel.PAAS);
        Collection<PlatformLevel> platformLevels = platformManager.getPlatformLevels();
        assertThat(platformLevels, IsIterableContainingInAnyOrder.containsInAnyOrder(PlatformLevel.PAAS, PlatformLevel.IAAS));
        assertThat(platformLevels, not(Matchers.hasItem(PlatformLevel.SAAS)));
        assertThat(platformLevels, not(Matchers.hasItem(PlatformLevel.OVERALL)));
    }

    @Test
    public void expirePlatformCachedRosters () {
        platformManager.expirePlatformCachedRosters();
        for (Platform platform : platformCollection) {
            verify(platform, times(1)).expireCachedRoster();
        }
    }

    @Test
    public void getNextPlatformForExperiments () {
        final Optional<Platform> nextPlatformForExperiment = platformManager.getNextPlatformForExperiment(false);
        assertTrue(nextPlatformForExperiment.isPresent());
        assertSame(nextPlatformForExperiment.get(), platform1);
    }

    @Test
    public void getNextPlatformForExperiments2 () {
        doReturn(false).when(platform1).hasEligibleContainersForExperiments();
        final Optional<Platform> nextPlatformForExperiment = platformManager.getNextPlatformForExperiment(false);
        assertTrue(nextPlatformForExperiment.isPresent());
        assertSame(nextPlatformForExperiment.get(), platform2);
    }

    @Test
    public void getNextPlatformForExperiments3 () {
        doReturn(false).when(platform2).canExperiment();
        doReturn(false).when(platform1).hasEligibleContainersForExperiments();
        final Optional<Platform> nextPlatformForExperiment = platformManager.getNextPlatformForExperiment(false);
        assertTrue(nextPlatformForExperiment.isEmpty());
    }

    @Test
    public void forceGetNextPlatformForExperiments () {
        doReturn(false).when(platform1).canExperiment();
        final Optional<Platform> nextPlatformForExperiment = platformManager.getNextPlatformForExperiment(true);
        assertTrue(nextPlatformForExperiment.isPresent());
        assertSame(nextPlatformForExperiment.get(), platform1);
    }

    @Test
    public void forceGetNextPlatformForExperiments2 () {
        doReturn(false).when(platform1).hasEligibleContainersForExperiments();
        final Optional<Platform> nextPlatformForExperiment = platformManager.getNextPlatformForExperiment(true);
        assertTrue(nextPlatformForExperiment.isPresent());
        assertSame(nextPlatformForExperiment.get(), platform2);
    }

    @Configuration
    static class ContextConfiguration {
        @Bean
        public PlatformManager platformManager () {
            return spy(new PlatformManager());
        }
    }
}