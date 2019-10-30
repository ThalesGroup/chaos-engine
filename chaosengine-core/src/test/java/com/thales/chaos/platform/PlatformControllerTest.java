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

package com.thales.chaos.platform;

import com.thales.chaos.platform.enums.PlatformHealth;
import com.thales.chaos.platform.enums.PlatformLevel;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.mockito.verification.VerificationMode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultMatcher;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Arrays;

import static com.thales.chaos.security.ChaosWebSecurityConfigurerAdapter.ADMIN_ROLE;
import static org.hamcrest.core.Is.is;
import static org.mockito.Mockito.*;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(properties = { "holidays=DUM", "scheduling=false" })
@AutoConfigureMockMvc
public class PlatformControllerTest {
    @MockBean
    private PlatformManager platformManager;
    @Autowired
    private MockMvc mvc;

    @Test
    @WithAdmin
    public void getPlatformHealthAsAdmin () throws Exception {
        doReturn(Arrays.asList(PlatformLevel.IAAS, PlatformLevel.PAAS)).when(platformManager).getPlatformLevels();
        Mockito.doReturn(PlatformHealth.OK).when(platformManager).getHealthOfPlatformLevel(PlatformLevel.IAAS);
        doReturn(PlatformHealth.DEGRADED).when(platformManager).getHealthOfPlatformLevel(PlatformLevel.PAAS);
        doReturn(PlatformHealth.DEGRADED).when(platformManager).getOverallHealth();
        mvc.perform(get("/platform/health"))
           .andExpect(status().isOk())
           .andExpect(jsonPath("$.IAAS", is(PlatformHealth.OK.toString())))
           .andExpect(jsonPath("$.PAAS", is(PlatformHealth.DEGRADED.toString())))
           .andExpect(jsonPath("$.OVERALL", is(PlatformHealth.DEGRADED.toString())))
           .andExpect(jsonPath("$.SAAS").doesNotExist());
    }

    @Test
    @WithGenericUser
    public void getPlatformHealthAsUser () throws Exception {
        doReturn(Arrays.asList(PlatformLevel.IAAS, PlatformLevel.PAAS)).when(platformManager).getPlatformLevels();
        Mockito.doReturn(PlatformHealth.OK).when(platformManager).getHealthOfPlatformLevel(PlatformLevel.IAAS);
        doReturn(PlatformHealth.DEGRADED).when(platformManager).getHealthOfPlatformLevel(PlatformLevel.PAAS);
        doReturn(PlatformHealth.DEGRADED).when(platformManager).getOverallHealth();
        mvc.perform(get("/platform/health"))
           .andExpect(status().isOk())
           .andExpect(jsonPath("$.IAAS", is(PlatformHealth.OK.toString())))
           .andExpect(jsonPath("$.PAAS", is(PlatformHealth.DEGRADED.toString())))
           .andExpect(jsonPath("$.OVERALL", is(PlatformHealth.DEGRADED.toString())))
           .andExpect(jsonPath("$.SAAS").doesNotExist());
    }

    @Test
    @WithAnonymousUser
    public void getPlatformHealthAsAnonymous () throws Exception {
        mvc.perform(get("/platform/health")).andExpect(status().isUnauthorized());
        verify(platformManager, never()).getOverallHealth();
    }

    @Test
    @WithAdmin
    public void getPlatformsAsAdmin () throws Exception {
        getPlatforms(status().isOk(), times(1));
    }

    private void getPlatforms (ResultMatcher unauthorized, VerificationMode never) throws Exception {
        mvc.perform(get("/platform")).andExpect(unauthorized);
        verify(platformManager, never).getPlatforms();
    }

    @Test
    @WithGenericUser
    public void getPlatformsAsUser () throws Exception {
        getPlatforms(status().isOk(), times(1));
    }

    @Test
    @WithAnonymousUser
    public void getPlatformsAsAnonymous () throws Exception {
        getPlatforms(status().isUnauthorized(), never());
    }

    @Test
    @WithAdmin
    public void expirePlatformRosterCacheAsAdmin () throws Exception {
        expirePlatformRosterCacheTestInner(status().isOk(), times(1));
    }

    private void expirePlatformRosterCacheTestInner (ResultMatcher forbidden, VerificationMode never) throws Exception {
        mvc.perform(post("/platform/refresh").contentType(APPLICATION_JSON)).andExpect(forbidden);
        verify(platformManager, never).expirePlatformCachedRosters();
    }

    @Test
    @WithGenericUser
    public void expirePlatformRosterCacheAsUser () throws Exception {
        expirePlatformRosterCacheTestInner(status().isForbidden(), never());
    }

    @Test
    @WithAnonymousUser
    public void expirePlatformRosterCacheAsAnonymous () throws Exception {
        expirePlatformRosterCacheTestInner(status().isUnauthorized(), never());
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