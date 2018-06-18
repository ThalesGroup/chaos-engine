package com.gemalto.chaos.health;

import com.gemalto.chaos.health.enums.SystemHealthState;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.junit.Assert.assertEquals;

@RunWith(MockitoJUnitRunner.class)
public class HealthControllerTest {

    @Mock
    private HealthManager healthManager;

    @Test
    public void getHealth() {
        HealthController hc = new HealthController(healthManager);

        Mockito.when(healthManager.getHealth()).thenReturn(SystemHealthState.OK);
        assertEquals(new ResponseEntity<>(SystemHealthState.OK, HttpStatus.OK), hc.getHealth());

        Mockito.when(healthManager.getHealth()).thenReturn(SystemHealthState.UNKNOWN);
        assertEquals(new ResponseEntity<>(SystemHealthState.UNKNOWN, HttpStatus.SERVICE_UNAVAILABLE), hc.getHealth());

        Mockito.when(healthManager.getHealth()).thenReturn(SystemHealthState.ERROR);
        assertEquals(new ResponseEntity<>(SystemHealthState.ERROR, HttpStatus.INTERNAL_SERVER_ERROR), hc.getHealth());

    }
}