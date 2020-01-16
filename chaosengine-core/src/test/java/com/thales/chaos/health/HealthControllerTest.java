/*
 *    Copyright (c) 2018 - 2020, Thales DIS CPL Canada, Inc
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 *
 */

package com.thales.chaos.health;

import com.thales.chaos.health.enums.SystemHealthState;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.web.servlet.MockMvc;

import java.lang.annotation.Retention;

import static com.thales.chaos.security.impl.ChaosWebSecurity.ChaosWebSecurityConfigurerAdapter.ADMIN_ROLE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(properties = "holidays=DUM")
@AutoConfigureMockMvc
public class HealthControllerTest {
    @Autowired
    private MockMvc mvc;
    @MockBean
    private HealthManager healthManager;

    @Test
    @WithAnonymousUser
    public void getHealthAsAnonymous () throws Exception {
        Mockito.when(healthManager.getHealth()).thenReturn(SystemHealthState.OK);
        mvc.perform(get("/health")).andExpect(status().isOk());
        Mockito.when(healthManager.getHealth()).thenReturn(SystemHealthState.UNKNOWN);
        mvc.perform(get("/health")).andExpect(status().is5xxServerError());
        Mockito.when(healthManager.getHealth()).thenReturn(SystemHealthState.ERROR);
        mvc.perform(get("/health")).andExpect(status().is5xxServerError());
    }

    @Test
    @WithAdmin
    public void getHealthAsAdmin () throws Exception {
        Mockito.when(healthManager.getHealth()).thenReturn(SystemHealthState.OK);
        mvc.perform(get("/health")).andExpect(status().isOk());
        Mockito.when(healthManager.getHealth()).thenReturn(SystemHealthState.UNKNOWN);
        mvc.perform(get("/health")).andExpect(status().is5xxServerError());
        Mockito.when(healthManager.getHealth()).thenReturn(SystemHealthState.ERROR);
        mvc.perform(get("/health")).andExpect(status().is5xxServerError());
    }

    @Retention(RUNTIME)
    @WithMockUser(roles = ADMIN_ROLE)
    private @interface WithAdmin {
    }


}