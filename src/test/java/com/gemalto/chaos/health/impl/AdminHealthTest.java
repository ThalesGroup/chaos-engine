package com.gemalto.chaos.health.impl;

import com.gemalto.chaos.admin.AdminManager;
import com.gemalto.chaos.health.enums.SystemHealthState;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.time.Duration;

import static com.gemalto.chaos.admin.enums.AdminState.STARTED;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.doReturn;

@RunWith(SpringJUnit4ClassRunner.class)
public class AdminHealthTest {
    @MockBean
    private AdminManager adminManager;
    @Autowired
    private AdminHealth adminHealth;


    @Test
    public void getHealth () {
        doReturn(STARTED).when(adminManager).getAdminState();
        doReturn(Duration.ZERO).when(adminManager).getTimeInState();
        assertEquals(SystemHealthState.OK, adminHealth.getHealth());
    }

    @Configuration
    static class TestConfig {
        @Bean
        AdminHealth adminHealth () {
            return Mockito.spy(new AdminHealth());
        }
    }
}