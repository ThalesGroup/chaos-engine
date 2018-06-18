package com.gemalto.chaos.health;

import com.gemalto.chaos.health.enums.SystemHealthState;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.Iterator;
import java.util.Set;

import static org.junit.Assert.assertEquals;

@RunWith(MockitoJUnitRunner.class)
public class HealthManagerTest {

    @Mock
    private Set<SystemHealth> systemHealthSet;
    @Mock
    private Iterator<SystemHealth> systemHealthIterator;
    @Mock
    private SystemHealth systemHealth;


    @Test
    public void getHealth() {
        HealthManager hm = new HealthManager(systemHealthSet);

        // One health class that returns OK
        Mockito.when(systemHealthSet.iterator()).thenReturn(systemHealthIterator);
        Mockito.when(systemHealthIterator.hasNext()).thenReturn(true).thenReturn(false);
        Mockito.when(systemHealthIterator.next()).thenReturn(systemHealth);
        Mockito.when(systemHealth.getHealth()).thenReturn(SystemHealthState.OK);

        assertEquals(SystemHealthState.OK, hm.getHealth());

        // Two health classes, both return OK
        Mockito.when(systemHealthSet.iterator()).thenReturn(systemHealthIterator);
        Mockito.when(systemHealthIterator.hasNext()).thenReturn(true).thenReturn(true).thenReturn(false);
        Mockito.when(systemHealthIterator.next()).thenReturn(systemHealth).thenReturn(systemHealth);
        Mockito.when(systemHealth.getHealth()).thenReturn(SystemHealthState.OK).thenReturn(SystemHealthState.OK);

        assertEquals(SystemHealthState.OK, hm.getHealth());

        // Two health classes, second returns ERROR
        Mockito.when(systemHealthSet.iterator()).thenReturn(systemHealthIterator);
        Mockito.when(systemHealthIterator.hasNext()).thenReturn(true).thenReturn(true).thenReturn(false);
        Mockito.when(systemHealthIterator.next()).thenReturn(systemHealth).thenReturn(systemHealth);
        Mockito.when(systemHealth.getHealth()).thenReturn(SystemHealthState.OK).thenReturn(SystemHealthState.ERROR);

        assertEquals(SystemHealthState.ERROR, hm.getHealth());

        // Two health classes, first returns ERROR
        Mockito.when(systemHealthSet.iterator()).thenReturn(systemHealthIterator);
        Mockito.when(systemHealthIterator.hasNext()).thenReturn(true).thenReturn(true).thenReturn(false);
        Mockito.when(systemHealthIterator.next()).thenReturn(systemHealth).thenReturn(systemHealth);
        Mockito.when(systemHealth.getHealth()).thenReturn(SystemHealthState.ERROR).thenReturn(SystemHealthState.OK);

        assertEquals(SystemHealthState.ERROR, hm.getHealth());


        // TODO : Test null SystemHealthSet, should return SystemHealthState.UNKNOWN


    }
}