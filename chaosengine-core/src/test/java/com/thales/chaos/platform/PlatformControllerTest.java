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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Arrays;

import static org.hamcrest.core.Is.is;
import static org.mockito.Mockito.*;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringJUnit4ClassRunner.class)
@WebMvcTest(PlatformController.class)
public class PlatformControllerTest {
    @MockBean
    private PlatformManager platformManager;
    @Autowired
    private MockMvc mvc;

    @Test
    public void getPlatformHealth () throws Exception {
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
    public void getPlatforms () throws Exception {
        mvc.perform(get("/platform")).andExpect(status().isOk());
        verify(platformManager, times(1)).getPlatforms();
    }

    @Test
    public void expirePlatformRosterCache () throws Exception {
        mvc.perform(post("/platform/refresh").contentType(APPLICATION_JSON)).andExpect(status().isOk());
        verify(platformManager, times(1)).expirePlatformCachedRosters();
    }
}