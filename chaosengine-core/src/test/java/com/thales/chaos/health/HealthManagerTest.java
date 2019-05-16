package com.thales.chaos.health;

import com.thales.chaos.health.enums.SystemHealthState;
import com.thales.chaos.health.impl.AdminHealth;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.*;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class HealthManagerTest {
    @MockBean
    private SystemHealth systemHealth;
    @MockBean
    private AdminHealth adminHealth;
    @Autowired
    private HealthManager healthManager;

    @Test
    public void getHealth () {
        when(systemHealth.getHealth()).thenReturn(SystemHealthState.OK);
        when(adminHealth.getHealth()).thenReturn(SystemHealthState.OK);
        assertEquals(SystemHealthState.OK, healthManager.getHealth());
        Mockito.reset(systemHealth, adminHealth);
        // Two health classes, second returns ERROR
        when(systemHealth.getHealth()).thenReturn(SystemHealthState.OK).thenReturn(SystemHealthState.ERROR);
        assertEquals(SystemHealthState.ERROR, healthManager.getHealth());
        Mockito.reset(systemHealth, adminHealth);
        // Two health classes, first returns ERROR
        when(systemHealth.getHealth()).thenReturn(SystemHealthState.ERROR).thenReturn(SystemHealthState.OK);
        assertEquals(SystemHealthState.ERROR, healthManager.getHealth());
        verify(systemHealth, times(1)).getHealth();
        Mockito.reset(systemHealth, adminHealth);
        // No health classes
        when(healthManager.getSystemHealth()).thenReturn(null);
        assertEquals(SystemHealthState.UNKNOWN, healthManager.getHealth());
    }

    @Configuration
    static class TestConfig {
        @Autowired
        private AdminHealth adminHealth;
        @Autowired
        private SystemHealth systemHealth;

        @Bean
        SystemHealth systemHealth () {
            return Mockito.mock(SystemHealth.class);
        }

        @Bean
        HealthManager healthManager () {
            return Mockito.spy(new HealthManager());
        }
    }
}