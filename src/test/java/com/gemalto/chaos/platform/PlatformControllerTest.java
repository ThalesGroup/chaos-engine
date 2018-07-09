package com.gemalto.chaos.platform;

import com.gemalto.chaos.platform.enums.PlatformHealth;
import com.gemalto.chaos.platform.enums.PlatformLevel;
import org.hamcrest.collection.IsMapContaining;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.Arrays;
import java.util.Map;

import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.doReturn;

@RunWith(MockitoJUnitRunner.class)
public class PlatformControllerTest {
    @Mock
    private PlatformManager platformManager;
    private PlatformController platformController;

    @Before
    public void setUp () {
        platformController = new PlatformController(platformManager);
    }

    @Test
    public void getPlatformHealth () {
        doReturn(Arrays.asList(PlatformLevel.IAAS, PlatformLevel.PAAS)).when(platformManager).getPlatformLevels();
        doReturn(PlatformHealth.OK).when(platformManager).getHealthOfPlatformLevel(PlatformLevel.IAAS);
        doReturn(PlatformHealth.DEGRADED).when(platformManager).getHealthOfPlatformLevel(PlatformLevel.PAAS);
        doReturn(PlatformHealth.DEGRADED).when(platformManager).getOverallHealth();
        Map<PlatformLevel, PlatformHealth> returnValue = platformController.getPlatformHealth();
        assertThat(returnValue, IsMapContaining.hasEntry(PlatformLevel.IAAS, PlatformHealth.OK));
        assertThat(returnValue, IsMapContaining.hasEntry(PlatformLevel.PAAS, PlatformHealth.DEGRADED));
        assertThat(returnValue, IsMapContaining.hasEntry(PlatformLevel.OVERALL, PlatformHealth.DEGRADED));
        assertThat(returnValue, not(hasKey(PlatformLevel.SAAS)));
    }
}