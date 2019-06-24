package com.thales.chaos.experiment;

import com.thales.chaos.admin.AdminManager;
import com.thales.chaos.calendar.HolidayManager;
import com.thales.chaos.container.Container;
import com.thales.chaos.container.enums.ContainerHealth;
import com.thales.chaos.experiment.annotations.NetworkExperiment;
import com.thales.chaos.experiment.annotations.StateExperiment;
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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.web.servlet.MockMvc;

import javax.validation.constraints.NotNull;
import java.util.List;
import java.util.Random;

import static java.util.UUID.randomUUID;
import static org.hamcrest.core.Is.is;
import static org.mockito.Mockito.*;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringJUnit4ClassRunner.class)
@WebMvcTest(ExperimentController.class)
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

        @StateExperiment
        public void restart (Experiment experiment) {
        }

        @NetworkExperiment
        public void latency (Experiment experiment) {
        }
    };

    @Before
    public void setUp () {
        experiment1 = GenericContainerExperiment.builder()
                                                .withContainer(container)
                                                .withExperimentType(ExperimentType.STATE)
                                                .build();
        experiment2 = GenericContainerExperiment.builder()
                                                .withContainer(container)
                                                .withExperimentType(ExperimentType.NETWORK)
                                                .build();
        autowireCapableBeanFactory.autowireBean(experiment1);
        autowireCapableBeanFactory.autowireBean(experiment2);
    }

    @Test
    public void getExperiments () throws Exception {
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
    public void getExperimentById () throws Exception {
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
    public void startExperiments () throws Exception {
        mvc.perform(post("/experiment/start").contentType(APPLICATION_JSON)).andExpect(status().isOk());
        verify(experimentManager, times(1)).scheduleExperiments(true);
    }

    @Test
    public void experimentContainerWithId () throws Exception {
        Long containerId = new Random().nextLong();
        mvc.perform(post("/experiment/start/" + containerId)).andExpect(status().isOk());
        verify(experimentManager, times(1)).experimentContainerId(containerId);
    }

    @Test
    public void startExperimentAlwaysReturnsExperiment () {
        experimentManager.scheduleExperiments(true);
    }
}