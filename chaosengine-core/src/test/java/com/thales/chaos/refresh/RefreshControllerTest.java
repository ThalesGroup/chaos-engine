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

package com.thales.chaos.refresh;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.web.servlet.MockMvc;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.List;

import static com.thales.chaos.security.impl.ChaosWebSecurity.ChaosWebSecurityConfigurerAdapter.ADMIN_ROLE;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(properties = "holidays=NONSTOP")
@AutoConfigureMockMvc
public class RefreshControllerTest {
    @MockBean
    private RefreshManager refreshManager;
    @Autowired
    private MockMvc mvc;

    @Test
    @WithAdmin
    public void doRefreshAsAdmin () throws Exception {
        doReturn(List.of("passwords", "keys")).when(refreshManager).doRefresh();
        mvc.perform(post("/refresh").contentType(MediaType.APPLICATION_JSON))
           .andExpect(status().isOk())
           .andExpect(content().string("[\"passwords\",\"keys\"]"));
    }

    @Test
    @WithGenericUser
    public void doRefreshAsUser () throws Exception {
        doReturn(List.of("passwords", "keys")).when(refreshManager).doRefresh();
        mvc.perform(post("/refresh").contentType(MediaType.APPLICATION_JSON)).andExpect(status().isForbidden());
    }

    @Test
    @WithAnonymousUser
    public void doRefreshAnonymous () throws Exception {
        doReturn(List.of("passwords", "keys")).when(refreshManager).doRefresh();
        mvc.perform(post("/refresh").contentType(MediaType.APPLICATION_JSON)).andExpect(status().isNotFound());
    }

    @Test
    @WithAdmin
    public void doRefreshFailure () throws Exception {
        doThrow(new RuntimeException()).when(refreshManager).doRefresh();
        mvc.perform(patch("/refresh").contentType(MediaType.APPLICATION_JSON)).andExpect(status().is5xxServerError());
    }

    @Test
    @WithAdmin
    public void doRestartAsAdmin () throws Exception {
        doReturn(true).when(refreshManager).doRestart();
        mvc.perform(post("/refresh/all").contentType(MediaType.APPLICATION_JSON))
           .andExpect(status().isOk())
           .andExpect(content().string("true"));
    }

    @Test
    @WithGenericUser
    public void doRestartAsUser () throws Exception {
        doReturn(true).when(refreshManager).doRestart();
        mvc.perform(post("/refresh/all").contentType(MediaType.APPLICATION_JSON)).andExpect(status().isForbidden());
    }

    @Test
    @WithAnonymousUser
    public void doRestartAnonymous () throws Exception {
        doReturn(true).when(refreshManager).doRestart();
        mvc.perform(post("/refresh/all").contentType(MediaType.APPLICATION_JSON)).andExpect(status().isNotFound());
    }

    @Test
    @WithAdmin
    public void doRestartFailure () throws Exception {
        doThrow(new RuntimeException()).when(refreshManager).doRestart();
        mvc.perform(patch("/refresh/all").contentType(MediaType.APPLICATION_JSON))
           .andExpect(status().is5xxServerError());
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