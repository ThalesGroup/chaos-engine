package com.gemalto.chaos.experiment;

import com.gemalto.chaos.experiment.enums.ExperimentType;
import com.gemalto.chaos.experiment.impl.GenericContainerExperiment;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Queue;
import java.util.Random;
import java.util.concurrent.LinkedBlockingDeque;

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
    private ExperimentManager experimentManager;
    private Experiment experiment1;
    private Experiment experiment2;

    @Before
    public void setUp () {
        experiment1 = GenericContainerExperiment.builder().withExperimentType(ExperimentType.STATE).build();
        experiment2 = GenericContainerExperiment.builder().withExperimentType(ExperimentType.NETWORK).build();
    }

    @Test
    public void getAttacks () throws Exception {
        when(experimentManager.getActiveExperiments()).thenReturn(new HashSet<>(Arrays.asList(experiment1, experiment2)));
        mvc.perform(get("/experiment").contentType(APPLICATION_JSON)).andExpect(status().isOk());
        //assertThat(attackController.getExperiments(), IsIterableContainingInAnyOrder.containsInAnyOrder(experiment1, experiment2));
    }

    @Test
    public void getAttackById () throws Exception {
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
    public void getAttackQueue () throws Exception {
        Queue<Experiment> attackQueue = new LinkedBlockingDeque<>();
        attackQueue.add(experiment1);
        when(experimentManager.getNewExperimentQueue()).thenReturn(attackQueue);
        mvc.perform(get("/experiment/queue").contentType(APPLICATION_JSON))
           .andExpect(status().isOk())
           .andExpect(jsonPath("$[0].experimentType", is("STATE")))
           .andExpect(jsonPath("$[0].startTime", is(experiment1.getStartTime().toString())));
    }

    @Test
    public void startAttacks () throws Exception {
        mvc.perform(post("/experiment/start").contentType(APPLICATION_JSON)).andExpect(status().isOk());
        verify(experimentManager, times(1)).startExperiments(true);
    }

    @Test
    public void attackContainerWithId () throws Exception {
        Long containerId = new Random().nextLong();
        mvc.perform(post("/experiment/start/" + containerId)).andExpect(status().isOk());
        verify(experimentManager, times(1)).experimentContainerId(containerId);
    }
}