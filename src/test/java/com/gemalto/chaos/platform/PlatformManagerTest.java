package com.gemalto.chaos.platform;

import com.gemalto.chaos.attack.enums.AttackType;
import com.gemalto.chaos.platform.enums.PlatformLevel;
import org.hamcrest.collection.IsIterableContainingInAnyOrder;
import org.javatuples.Pair;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;

import static com.gemalto.chaos.platform.enums.PlatformHealth.*;
import static com.gemalto.chaos.platform.enums.PlatformLevel.*;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.*;
import static org.springframework.test.util.AssertionErrors.assertTrue;

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
        when(platform1.getPlatformLevel()).thenReturn(IAAS);
        when(platform2.getPlatformLevel()).thenReturn(PAAS);
        Collection<Platform> iaasPlatforms = platformManager.getPlatformsOfLevel(IAAS);
        Collection<Platform> paasPlatforms = platformManager.getPlatformsOfLevel(PAAS);
        assertThat(paasPlatforms, not(hasItem(platform1)));
        assertThat(iaasPlatforms, not(hasItem(platform2)));
        assertThat(iaasPlatforms, IsIterableContainingInAnyOrder.containsInAnyOrder(platform1));
        assertThat(paasPlatforms, IsIterableContainingInAnyOrder.containsInAnyOrder(platform2));
    }

    @Test
    public void getOverallHealth () {
        // Two different healths
        when(platform1.getPlatformHealth()).thenReturn(OK);
        when(platform2.getPlatformHealth()).thenReturn(DEGRADED);
        assertEquals(DEGRADED, platformManager.getOverallHealth());
        // Two different healths in reverse order
        when(platform1.getPlatformHealth()).thenReturn(DEGRADED);
        when(platform2.getPlatformHealth()).thenReturn(OK);
        // A different set of two healths
        assertEquals(DEGRADED, platformManager.getOverallHealth());
        when(platform1.getPlatformHealth()).thenReturn(DEGRADED);
        when(platform2.getPlatformHealth()).thenReturn(FAILED);
        // A final set of two healths.
        assertEquals(FAILED, platformManager.getOverallHealth());
        when(platform1.getPlatformHealth()).thenReturn(FAILED);
        when(platform2.getPlatformHealth()).thenReturn(OK);
        assertEquals(FAILED, platformManager.getOverallHealth());
        // Two Equal healths
        when(platform1.getPlatformHealth()).thenReturn(FAILED);
        when(platform2.getPlatformHealth()).thenReturn(FAILED);
        assertEquals(FAILED, platformManager.getOverallHealth());
        when(platform1.getPlatformHealth()).thenReturn(OK);
        when(platform2.getPlatformHealth()).thenReturn(OK);
        assertEquals(OK, platformManager.getOverallHealth());
    }

    @Test
    public void getHealthOfPlatformLevel () {
        when(platform1.getPlatformLevel()).thenReturn(PAAS);
        when(platform1.getPlatformHealth()).thenReturn(DEGRADED);
        when(platform2.getPlatformLevel()).thenReturn(IAAS);
        when(platform2.getPlatformHealth()).thenReturn(FAILED);
        assertEquals(DEGRADED, platformManager.getHealthOfPlatformLevel(PAAS));
        assertEquals(FAILED, platformManager.getHealthOfPlatformLevel(IAAS));
    }

    @Test
    public void getPlatformLevels () {
        when(platform1.getPlatformLevel()).thenReturn(IAAS);
        when(platform2.getPlatformLevel()).thenReturn(PAAS);
        Collection<PlatformLevel> platformLevels = platformManager.getPlatformLevels();
        assertThat(platformLevels, IsIterableContainingInAnyOrder.containsInAnyOrder(PAAS, IAAS));
        assertThat(platformLevels, not(hasItem(SAAS)));
        assertThat(platformLevels, not(hasItem(OVERALL)));
    }

    @Test
    public void getAttackPlatformAndType () {
        doReturn(Collections.singletonList(AttackType.STATE)).when(platform1).getSupportedAttackTypes();
        doReturn(Collections.singletonList(AttackType.NETWORK)).when(platform2).getSupportedAttackTypes();
        assertTrue("Unexpected Attack Platform and Type: " + platformManager.getAttackPlatformAndType(), platformManager
                .getAttackPlatformAndType()
                .equals(new Pair<>(platform1, AttackType.STATE)) || platformManager.getAttackPlatformAndType()
                                                                                   .equals(new Pair<>(platform2, AttackType.NETWORK)));
        verify(platform1, times(1)).getSupportedAttackTypes();
        verify(platform2, times(1)).getSupportedAttackTypes();
    }
}