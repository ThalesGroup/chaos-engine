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

package com.thales.chaos.experiment;

import com.thales.chaos.admin.AdminManager;
import com.thales.chaos.calendar.HolidayManager;
import com.thales.chaos.container.Container;
import com.thales.chaos.container.enums.ContainerHealth;
import com.thales.chaos.experiment.annotations.ChaosExperiment;
import com.thales.chaos.experiment.enums.ExperimentType;
import com.thales.chaos.experiment.impl.GenericContainerExperiment;
import com.thales.chaos.notification.NotificationManager;
import com.thales.chaos.notification.datadog.DataDogIdentifier;
import com.thales.chaos.platform.Platform;
import com.thales.chaos.scripts.ScriptManager;
import org.hamcrest.core.Is;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.verification.VerificationMode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultMatcher;

import javax.validation.constraints.NotNull;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.time.Duration;
import java.util.*;

import static com.thales.chaos.experiment.ExperimentSuite.fromMap;
import static com.thales.chaos.security.impl.ChaosWebSecurity.ChaosWebSecurityConfigurerAdapter.ADMIN_ROLE;
import static java.util.UUID.randomUUID;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.*;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.http.MediaType.APPLICATION_JSON_UTF8;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(properties = "holidays=DUM")
@AutoConfigureMockMvc
public class ExperimentControllerTest {
    @Autowired
    private MockMvc mvc;
    @MockBean
    private HolidayManager holidayManager;
    @MockBean
    private ExperimentManager experimentManager;
    @MockBean
    private NotificationManager notificationManager;
    @SpyBean
    private AdminManager adminManager;
    @MockBean
    private ScriptManager scriptManager;
    @Autowired
    private AutowireCapableBeanFactory autowireCapableBeanFactory;
    private Experiment experiment1;
    private Experiment experiment2;
    private Container container = new Container() {
        @Override
        public Platform getPlatform () {
            return null;
        }

        @Override
        protected ContainerHealth updateContainerHealthImpl (ExperimentType experimentType) {
            return ContainerHealth.NORMAL;
        }

        @Override
        public String getSimpleName () {
            return null;
        }

        @Override
        public String getAggregationIdentifier () {
            return null;
        }

        @Override
        public DataDogIdentifier getDataDogIdentifier () {
            return null;
        }

        @Override
        protected boolean compareUniqueIdentifierInner (@NotNull String uniqueIdentifier) {
            return false;
        }

        @ChaosExperiment(experimentType = ExperimentType.STATE)
        public void restart (Experiment experiment) {
        }

        @ChaosExperiment(experimentType = ExperimentType.NETWORK)
        public void latency (Experiment experiment) {
        }
    };

    @Before
    public void setUp () {
        experiment1 = GenericContainerExperiment.builder().withContainer(container).withSpecificExperiment("restart").build();
        experiment2 = GenericContainerExperiment.builder().withContainer(container).withSpecificExperiment("latency").build();
        autowireCapableBeanFactory.autowireBean(experiment1);
        autowireCapableBeanFactory.autowireBean(experiment2);
        experiment1.setScriptManager(scriptManager);
        experiment2.setScriptManager(scriptManager);
    }

    @Test
    @WithAdmin
    public void getExperimentsAsAdmin () throws Exception {
        doReturn(List.of(experiment1)).when(experimentManager).getAllExperiments();
        mvc.perform(get("/experiment").contentType(APPLICATION_JSON))
           .andExpect(status().isOk())
           .andExpect(jsonPath("$[0].id", is(experiment1.getId())))
           .andExpect(jsonPath("$[0].experimentMethodName", Is.is("restart")));
        doReturn(List.of(experiment2)).when(experimentManager).getAllExperiments();
        mvc.perform(get("/experiment").contentType(APPLICATION_JSON))
           .andExpect(status().isOk())
           .andExpect(jsonPath("$[0].id", is(experiment2.getId())))
           .andExpect(jsonPath("$[0].experimentMethodName", Is.is("latency")));
    }

    @Test
    @WithGenericUser
    public void getExperimentsAsUser () throws Exception {
        doReturn(List.of(experiment1)).when(experimentManager).getAllExperiments();
        mvc.perform(get("/experiment").contentType(APPLICATION_JSON))
           .andExpect(status().isOk())
           .andExpect(jsonPath("$[0].id", is(experiment1.getId())))
           .andExpect(jsonPath("$[0].experimentMethodName", Is.is("restart")));
        doReturn(List.of(experiment2)).when(experimentManager).getAllExperiments();
        mvc.perform(get("/experiment").contentType(APPLICATION_JSON))
           .andExpect(status().isOk())
           .andExpect(jsonPath("$[0].id", is(experiment2.getId())))
           .andExpect(jsonPath("$[0].experimentMethodName", Is.is("latency")));
    }

    @Test
    @WithAnonymousUser
    public void getExperimentsAsAnonymousUser () throws Exception {
        mvc.perform(get("/experiment").contentType(APPLICATION_JSON)).andExpect(status().isNotFound());
    }

    @Test
    @WithAdmin
    public void getExperimentByIdAsAdmin () throws Exception {
        String UUID1 = randomUUID().toString();
        String UUID2 = randomUUID().toString();
        when(experimentManager.getExperimentByUUID(UUID1)).thenReturn(experiment1);
        when(experimentManager.getExperimentByUUID(UUID2)).thenReturn(experiment2);
        mvc.perform(get("/experiment/" + UUID1).contentType(APPLICATION_JSON))
           .andExpect(status().isOk())
           .andExpect(jsonPath("$.experimentType", is("STATE")))
           .andExpect(jsonPath("$.startTime", is(experiment1.getStartTime().toString())));
        mvc.perform(get("/experiment/" + UUID2).contentType(APPLICATION_JSON))
           .andExpect(status().isOk())
           .andExpect(jsonPath("$.experimentType", is("NETWORK")))
           .andExpect(jsonPath("$.startTime", is(experiment2.getStartTime().toString())));
    }

    @Test
    @WithGenericUser
    public void getExperimentByIdAsUser () throws Exception {
        String UUID1 = randomUUID().toString();
        String UUID2 = randomUUID().toString();
        when(experimentManager.getExperimentByUUID(UUID1)).thenReturn(experiment1);
        when(experimentManager.getExperimentByUUID(UUID2)).thenReturn(experiment2);
        mvc.perform(get("/experiment/" + UUID1).contentType(APPLICATION_JSON))
           .andExpect(status().isOk())
           .andExpect(jsonPath("$.experimentType", is("STATE")))
           .andExpect(jsonPath("$.startTime", is(experiment1.getStartTime().toString())));
        mvc.perform(get("/experiment/" + UUID2).contentType(APPLICATION_JSON))
           .andExpect(status().isOk())
           .andExpect(jsonPath("$.experimentType", is("NETWORK")))
           .andExpect(jsonPath("$.startTime", is(experiment2.getStartTime().toString())));
    }

    @Test
    @WithAnonymousUser
    public void getExperimentByIdAsAnonymousUser () throws Exception {
        mvc.perform(get("/experiment/" + randomUUID().toString()).contentType(APPLICATION_JSON))
           .andExpect(status().isNotFound());
        verify(experimentManager, never()).getExperimentByUUID(any());
    }

    @Test
    @WithAdmin
    public void startExperimentsAsAdmin () throws Exception {
        startExperimentsTestInner(status().isOk(), times(1));
    }

    private void startExperimentsTestInner (ResultMatcher result, VerificationMode verification) throws Exception {
        mvc.perform(post("/experiment/start").contentType(APPLICATION_JSON)).andExpect(result);
        verify(experimentManager, verification).scheduleExperiments(true);
    }

    @Test
    @WithGenericUser
    public void startExperimentsAsUser () throws Exception {
        startExperimentsTestInner(status().isForbidden(), never());
    }

    @Test
    @WithAnonymousUser
    public void startExperimentsAsAnonymous () throws Exception {
        startExperimentsTestInner(status().isNotFound(), never());
    }

    @Test
    @WithAdmin
    public void experimentContainerWithIdAsAdmin () throws Exception {
        experimentContainerWithIdTestInner(status().isOk(), times(1));
    }

    private void experimentContainerWithIdTestInner (ResultMatcher result, VerificationMode verification) throws Exception {
        Long containerId = new Random().nextLong();
        mvc.perform(post("/experiment/start/" + containerId)).andExpect(result);
        verify(experimentManager, verification).experimentContainerId(containerId);
    }

    @Test
    @WithGenericUser
    public void experimentContainerWithIdAsUser () throws Exception {
        experimentContainerWithIdTestInner(status().isForbidden(), never());
    }

    @Test
    @WithAnonymousUser
    public void experimentContainerWithIdAsAnonymous () throws Exception {
        experimentContainerWithIdTestInner(status().isNotFound(), never());
    }

    @Test
    @WithAdmin
    public void startExperimentSuiteAsAdmin () throws Exception {
        Collection<Experiment> experiments = Collections.emptySet();
        ArgumentCaptor<ExperimentSuite> experimentSuiteCaptor = ArgumentCaptor.forClass(ExperimentSuite.class);
        doReturn(experiments).when(experimentManager).scheduleExperimentSuite(experimentSuiteCaptor.capture());
        ExperimentSuite expectedExperimentSuite = new ExperimentSuite("firstPlatform", fromMap(Map.of("application", List
                .of("method1", "method2"))));
        mvc.perform(post("/experiment/build").contentType(APPLICATION_JSON_UTF8).content(expectedExperimentSuite.toString())).andExpect(status().isOk());
        startExperimentSuiteTestInner(status().isOk(), expectedExperimentSuite);
        assertEquals(expectedExperimentSuite, experimentSuiteCaptor.getValue());
    }

    private void startExperimentSuiteTestInner (ResultMatcher result, ExperimentSuite expectedExperimentSuite) throws Exception {
        mvc.perform(post("/experiment/build").contentType(APPLICATION_JSON_UTF8)
                                             .content(expectedExperimentSuite.toString())).andExpect(result);
    }

    @Test
    @WithGenericUser
    public void startExperimentSuiteAsUser () throws Exception {
        startExperimentSuiteTestInner(status().isForbidden());
    }

    private void startExperimentSuiteTestInner (ResultMatcher result) throws Exception {
        startExperimentSuiteTestInner(result, new ExperimentSuite("firstPlatform", fromMap(Map.of("application", List.of("method1", "method2")))));
    }

    @Test
    @WithAnonymousUser
    public void startExperimentSuiteAsAnonymous () throws Exception {
        startExperimentSuiteTestInner(status().isNotFound());
    }

    @Test
    @WithAdmin
    public void isAutomatedModeAsAdmin () throws Exception {
        mvc.perform(get("/experiment/automated").contentType(APPLICATION_JSON)).andExpect(status().isOk());
    }

    @Test
    @WithGenericUser
    public void isAutomatedModeAsUser () throws Exception {
        mvc.perform(get("/experiment/automated").contentType(APPLICATION_JSON)).andExpect(status().isOk());
    }

    @Test
    @WithAnonymousUser
    public void isAutomatedModeAsAnonymous () throws Exception {
        isAutomatedModeTestInner(status().isNotFound());
    }

    private void isAutomatedModeTestInner (ResultMatcher result) throws Exception {
        mvc.perform(get("/experiment/automated").contentType(APPLICATION_JSON)).andExpect(result);
    }

    @Test
    @WithAdmin
    public void enableAutomatedModeAsAdmin () throws Exception {
        enableAutomatedModeTestInner(status().isOk(), times(1));
    }

    private void enableAutomatedModeTestInner (ResultMatcher ok, VerificationMode times) throws Exception {
        mvc.perform(post("/experiment/automated").contentType(APPLICATION_JSON)).andExpect(ok);
        verify(experimentManager, times).setAutomatedMode(true);
    }

    @Test
    @WithGenericUser
    public void enableAutomatedModeAsUser () throws Exception {
        enableAutomatedModeTestInner(status().isForbidden(), never());
    }

    @Test
    @WithAnonymousUser
    public void enableAutomatedModeAsAnonymous () throws Exception {
        enableAutomatedModeTestInner(status().isNotFound(), never());
    }

    @Test
    @WithAdmin
    public void disableAutomatedModeAsAdmin () throws Exception {
        disableAutomatedModeTestInner(status().isOk(), times(1));
    }

    private void disableAutomatedModeTestInner (ResultMatcher forbidden, VerificationMode never) throws Exception {
        mvc.perform(delete("/experiment/automated").contentType(APPLICATION_JSON)).andExpect(forbidden);
        verify(experimentManager, never).setAutomatedMode(false);
    }

    @Test
    @WithGenericUser
    public void disableAutomatedModeAsUser () throws Exception {
        disableAutomatedModeTestInner(status().isForbidden(), never());
    }

    @Test
    @WithAnonymousUser
    public void disableAutomatedModeAsAnonymous () throws Exception {
        disableAutomatedModeTestInner(status().isNotFound(), never());
    }

    @Test
    @WithAdmin
    public void setBackoffPeriodAsAdmin () throws Exception {
        setBackoffPeriodTestInner(status().isOk(), times(1));
    }

    private void setBackoffPeriodTestInner (ResultMatcher result, VerificationMode verification) throws Exception {
        for (int i = 100; i < 1000; i = i + 100) {
            Duration duration = Duration.ofSeconds(i);
            mvc.perform(patch("/experiment/backoff").contentType(APPLICATION_JSON_UTF8)
                                                    .param("backoffDuration", duration.toString())).andExpect(result);
            verify(experimentManager, verification).setExperimentBackoffPeriod(duration);
        }
    }

    @Test
    @WithGenericUser
    public void setBackoffPeriodAsUser () throws Exception {
        setBackoffPeriodTestInner(status().isForbidden(), never());
    }

    @Test
    @WithAnonymousUser
    public void setBackoffPeriodWithUser () throws Exception {
        setBackoffPeriodTestInner(status().isNotFound(), never());
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