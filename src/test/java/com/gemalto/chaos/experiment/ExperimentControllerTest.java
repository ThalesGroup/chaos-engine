package com.gemalto.chaos.experiment;

import com.gemalto.chaos.admin.AdminManager;
import com.gemalto.chaos.container.Container;
import com.gemalto.chaos.container.enums.ContainerHealth;
import com.gemalto.chaos.experiment.annotations.NetworkExperiment;
import com.gemalto.chaos.experiment.annotations.StateExperiment;
import com.gemalto.chaos.experiment.enums.ExperimentType;
import com.gemalto.chaos.experiment.impl.GenericContainerExperiment;
import com.gemalto.chaos.notification.NotificationManager;
import com.gemalto.chaos.notification.datadog.DataDogIdentifier;
import com.gemalto.chaos.platform.Platform;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.web.servlet.MockMvc;

import javax.validation.constraints.NotNull;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Queue;
import java.util.Random;
import java.util.concurrent.LinkedBlockingDeque;

import static com.gemalto.chaos.constants.ExperimentConstants.EXPERIMENT_METHOD_NOT_SET_YET;
import static java.util.UUID.randomUUID;
import static org.hamcrest.collection.IsIn.isIn;
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
    private ExperimentManager experimentManager;
    @MockBean
    private NotificationManager notificationManager;
    @MockBean
    private AdminManager adminManager;
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
        when(experimentManager.getActiveExperiments()).thenReturn(new HashSet<>(Arrays.asList(experiment1, experiment2)));
        //Scheduled experiments
        mvc.perform(get("/experiment").contentType(APPLICATION_JSON))
           .andExpect(status().isOk())
           .andExpect(jsonPath("$[0].id", isIn(Arrays.asList(experiment1.getId(), experiment2.getId()))))
           .andExpect(jsonPath("$[1].id", isIn(Arrays.asList(experiment1.getId(), experiment2.getId()))))
           .andExpect(jsonPath("$[0].experimentMethodName", is(EXPERIMENT_METHOD_NOT_SET_YET)))
           .andExpect(jsonPath("$[1].experimentMethodName", is(EXPERIMENT_METHOD_NOT_SET_YET)));
        //Running experiments
        doReturn(true).when(adminManager).canRunExperiments();
        experiment1.startExperiment();
        experiment2.startExperiment();
        mvc.perform(get("/experiment").contentType(APPLICATION_JSON))
           .andExpect(status().isOk())
           .andExpect(jsonPath("$[0].id", isIn(Arrays.asList(experiment1.getId(), experiment2.getId()))))
           .andExpect(jsonPath("$[1].id", isIn(Arrays.asList(experiment1.getId(), experiment2.getId()))))
           .andExpect(jsonPath("$[0].experimentMethodName", isIn(Arrays.asList("restart", "latency"))))
           .andExpect(jsonPath("$[1].experimentMethodName", isIn(Arrays.asList("restart", "latency"))));
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
    public void getExperimentQueue () throws Exception {
        Queue<Experiment> experimentQueue = new LinkedBlockingDeque<>();
        experimentQueue.add(experiment1);
        when(experimentManager.getNewExperimentQueue()).thenReturn(experimentQueue);
        mvc.perform(get("/experiment/queue").contentType(APPLICATION_JSON))
           .andExpect(status().isOk())
           .andExpect(jsonPath("$[0].experimentType", is("STATE")))
           .andExpect(jsonPath("$[0].startTime", is(experiment1.getStartTime().toString())));
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
}