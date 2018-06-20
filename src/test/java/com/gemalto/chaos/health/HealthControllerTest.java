package com.gemalto.chaos.health;

import com.gemalto.chaos.health.enums.SystemHealthState;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import static org.junit.Assert.assertEquals;

@RunWith(MockitoJUnitRunner.class)
public class HealthControllerTest {

    @Mock
    private HealthManager healthManager;

    @Rule
    public final ExpectedException expectedException = ExpectedException.none();

    @Test
    public void getHealth() {
        HealthController hc = new HealthController(healthManager);

        Mockito.when(healthManager.getHealth()).thenReturn(SystemHealthState.OK);
        assertEquals(SystemHealthState.OK, hc.getHealth());

        Mockito.when(healthManager.getHealth()).thenReturn(SystemHealthState.UNKNOWN);
        expectedException.expect(HealthController.HealthUnknownException.class);
        hc.getHealth();

        Mockito.when(healthManager.getHealth()).thenReturn(SystemHealthState.ERROR);
        expectedException.expect(HealthController.HealthErrorException.class);
        hc.getHealth();

    }
}