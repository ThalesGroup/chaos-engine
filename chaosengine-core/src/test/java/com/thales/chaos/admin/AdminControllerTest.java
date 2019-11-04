/*
 *    Copyright (c) 2019 Thales Group
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

package com.thales.chaos.admin;

import com.thales.chaos.admin.enums.AdminState;
import com.thales.chaos.notification.NotificationManager;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.web.servlet.MockMvc;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import static com.thales.chaos.security.impl.ChaosWebSecurityConfigurerAdapter.ADMIN_ROLE;
import static org.hamcrest.core.Is.is;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(properties = "holidays=DUM")
@AutoConfigureMockMvc
public class AdminControllerTest {
    @MockBean
    private NotificationManager notificationManager;
    @SpyBean
    private AdminManager adminManager;
    @Autowired
    private MockMvc mvc;

    @Before
    public void beforeEach () {
        Mockito.doReturn(AdminState.STARTED).when(adminManager).getAdminState();
    }

    @Test
    @WithAdmin
    public void getAdminStateAsAdmin () throws Exception {
        mvc.perform(get("/admin" + "/state").contentType(APPLICATION_JSON)).andExpect(status().isOk()).andExpect(jsonPath("$", is("STARTED")));
    }

    @Test
    @WithGenericUser
    public void getAdminStateAsUser () throws Exception {
        mvc.perform(get("/admin" + "/state").contentType(APPLICATION_JSON)).andExpect(status().isOk());
    }

    @Test
    @WithAnonymousUser
    public void getAdminStateWithoutAuthentication () throws Exception {
        mvc.perform(get("/admin" + "/state").contentType(APPLICATION_JSON)).andExpect(status().isNotFound());
    }

    @Test
    @WithAdmin
    public void setAdminStateAsAdmin () throws Exception {
        mvc.perform(post("/admin" + "/state").contentType(APPLICATION_JSON).param("state", "BOGUS STATE")).andExpect(status().is5xxServerError());
        mvc.perform(post("/admin" + "/state").contentType(APPLICATION_JSON).param("state", "PAUSED")).andExpect(status().isOk());
    }

    @Test
    @WithGenericUser
    public void setAdminStateAsUser () throws Exception {
        mvc.perform(post("/admin" + "/state").contentType(APPLICATION_JSON).param("state", "BOGUS STATE"))
           .andExpect(status().isForbidden());
        mvc.perform(post("/admin" + "/state").contentType(APPLICATION_JSON).param("state", "PAUSED"))
           .andExpect(status().isForbidden());
    }

    @Test
    @WithAnonymousUser
    public void setAdminStateWithoutAuthentication () throws Exception {
        mvc.perform(post("/admin" + "/state").contentType(APPLICATION_JSON).param("state", "BOGUS STATE"))
           .andExpect(status().isNotFound());
        mvc.perform(post("/admin" + "/state").contentType(APPLICATION_JSON).param("state", "PAUSED"))
           .andExpect(status().isNotFound());
    }

    @Retention(RetentionPolicy.RUNTIME)
    @WithMockUser(roles = ADMIN_ROLE)
    private @interface WithAdmin {
    }

    @Retention(RetentionPolicy.RUNTIME)
    @WithMockUser
    private @interface WithGenericUser {
    }
}