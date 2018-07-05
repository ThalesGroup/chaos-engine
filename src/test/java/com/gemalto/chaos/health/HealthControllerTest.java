package com.gemalto.chaos.health;

import com.gemalto.chaos.health.enums.SystemHealthState;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import static org.junit.Assert.assertEquals;

@RunWith(MockitoJUnitRunner.class)
public class HealthControllerTest {
    @Mock
    private HealthManager healthManager;
    private HealthController hc;

    @Before
    public void setUp () {
        hc = new HealthController(healthManager);
    }

    @Test
    public void getHealthNormal () {
        Mockito.when(healthManager.getHealth()).thenReturn(SystemHealthState.OK);
        assertEquals(SystemHealthState.OK, hc.getHealth());
    }

    @Test(expected = HealthController.HealthUnknownException.class)
    public void getHealthUnknown () {
        Mockito.when(healthManager.getHealth()).thenReturn(SystemHealthState.UNKNOWN);
        hc.getHealth();
    }

    @Test(expected = HealthController.HealthErrorException.class)
    public void getHealthError () {
        Mockito.when(healthManager.getHealth()).thenReturn(SystemHealthState.ERROR);
        hc.getHealth();
    }
}