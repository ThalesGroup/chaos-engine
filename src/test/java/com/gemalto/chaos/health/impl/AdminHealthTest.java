package com.gemalto.chaos.health.impl;

import com.gemalto.chaos.admin.AdminManager;
import com.gemalto.chaos.admin.enums.AdminState;
import com.gemalto.chaos.health.enums.SystemHealthState;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.concurrent.TimeUnit;

import static org.awaitility.Awaitility.await;
import static org.junit.Assert.assertEquals;

@RunWith(SpringJUnit4ClassRunner.class)
public class AdminHealthTest {
    private final AdminHealth adminHealth = new AdminHealth();

    @Before
    public void setUp () {
        while (AdminManager.getAdminState() != AdminState.STARTED) {
            AdminManager.setAdminState(AdminState.STARTED);
            await().atMost(1, TimeUnit.SECONDS)
                   .untilAsserted(() -> assertEquals(AdminState.STARTED, AdminManager.getAdminState()));
        }
    }

    @Test
    public void getHealth () {
        assertEquals(SystemHealthState.OK, adminHealth.getHealth());
    }
}