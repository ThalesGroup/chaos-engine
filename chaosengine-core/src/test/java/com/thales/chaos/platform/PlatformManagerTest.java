package com.thales.chaos.platform;

import com.thales.chaos.platform.enums.PlatformLevel;
import com.thales.chaos.platform.enums.PlatformHealth;
import org.hamcrest.Matchers;
import org.hamcrest.collection.IsIterableContainingInAnyOrder;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class PlatformManagerTest {
    @Mock
    private Platform platform1;
    @Mock
    private Platform platform2;
    private Collection<Platform> platformCollection;
    private PlatformManager platformManager;

    @Before
    public void setUp () {
        platformCollection = new HashSet<>(Arrays.asList(platform1, platform2));
        platformManager = new PlatformManager(platformCollection);
    }

    @Test
    public void getPlatforms () {
        assertThat(platformManager.getPlatforms(), IsIterableContainingInAnyOrder.containsInAnyOrder(platformCollection.toArray()));
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
}