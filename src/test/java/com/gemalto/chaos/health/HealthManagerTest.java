package com.gemalto.chaos.health;

import com.gemalto.chaos.health.enums.SystemHealthState;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class HealthManagerTest {
    @Mock
    private Collection<SystemHealth> systemHealthSet;
    @Mock
    private Iterator<SystemHealth> systemHealthIterator;
    @Mock
    private SystemHealth systemHealth;
    private HealthManager hm;

    @Test
    public void getHealth () {
        hm = new HealthManager(Collections.singleton(systemHealth));
        // One health class that returns OK
        when(systemHealth.getHealth()).thenReturn(SystemHealthState.OK);
        assertEquals(SystemHealthState.OK, hm.getHealth());
    }

    @Test
    public void getHealth2 () {
        hm = new HealthManager(Arrays.asList(systemHealth, systemHealth));
        // Two health classes, both return OK
        when(systemHealth.getHealth()).thenReturn(SystemHealthState.OK).thenReturn(SystemHealthState.OK);
        assertEquals(SystemHealthState.OK, hm.getHealth());
    }

    @Test
    public void getHealth3 () {
        hm = new HealthManager(Arrays.asList(systemHealth, systemHealth));
        // Two health classes, second returns ERROR
        when(systemHealth.getHealth()).thenReturn(SystemHealthState.OK).thenReturn(SystemHealthState.ERROR);
        assertEquals(SystemHealthState.ERROR, hm.getHealth());
    }

    @Test
    public void getHealth4 () {
        hm = new HealthManager(Arrays.asList(systemHealth, systemHealth));
        // Two health classes, first returns ERROR
        when(systemHealth.getHealth()).thenReturn(SystemHealthState.ERROR).thenReturn(SystemHealthState.OK);
        assertEquals(SystemHealthState.ERROR, hm.getHealth());
        verify(systemHealth, times(1)).getHealth();
    }

    @Test
    public void getHealth5 () {
        hm = new HealthManager(null);
        // No health classes
        assertEquals(SystemHealthState.UNKNOWN, hm.getHealth());
    }
}