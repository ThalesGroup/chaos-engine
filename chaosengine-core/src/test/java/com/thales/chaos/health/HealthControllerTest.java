package com.thales.chaos.health;

import com.thales.chaos.health.enums.SystemHealthState;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringJUnit4ClassRunner.class)
@WebMvcTest(HealthController.class)
public class HealthControllerTest {
    @Autowired
    private MockMvc mvc;
    @MockBean
    private HealthManager healthManager;

    @Test
    public void getHealth () throws Exception {
        Mockito.when(healthManager.getHealth()).thenReturn(SystemHealthState.OK);
        mvc.perform(get("/health")).andExpect(status().isOk());
        Mockito.when(healthManager.getHealth()).thenReturn(SystemHealthState.UNKNOWN);
        mvc.perform(get("/health")).andExpect(status().is5xxServerError());
        Mockito.when(healthManager.getHealth()).thenReturn(SystemHealthState.ERROR);
        mvc.perform(get("/health")).andExpect(status().is5xxServerError());
    }
}