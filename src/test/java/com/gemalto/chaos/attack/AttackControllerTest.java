package com.gemalto.chaos.attack;

import com.gemalto.chaos.attack.enums.AttackType;
import com.gemalto.chaos.attack.impl.GenericContainerAttack;
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
@WebMvcTest(AttackController.class)
public class AttackControllerTest {
    @Autowired
    private MockMvc mvc;
    @MockBean
    private AttackManager attackManager;

    private Attack attack1;
    private Attack attack2;

    @Before
    public void setUp () {
        attack1 = GenericContainerAttack.builder().withAttackType(AttackType.STATE).build();
        attack2 = GenericContainerAttack.builder().withAttackType(AttackType.NETWORK).build();
    }

    @Test
    public void getAttacks () throws Exception {
        when(attackManager.getActiveAttacks()).thenReturn(new HashSet<>(Arrays.asList(attack1, attack2)));
        mvc.perform(get("/attack").contentType(APPLICATION_JSON)).andExpect(status().isOk());
        //assertThat(attackController.getAttacks(), IsIterableContainingInAnyOrder.containsInAnyOrder(attack1, attack2));
    }

    @Test
    public void getAttackById () throws Exception {
        String UUID1 = randomUUID().toString();
        String UUID2 = randomUUID().toString();
        when(attackManager.getAttackByUUID(UUID1)).thenReturn(attack1);
        when(attackManager.getAttackByUUID(UUID2)).thenReturn(attack2);
        mvc.perform(get("/attack/" + UUID1).contentType(APPLICATION_JSON))
           .andExpect(status().isOk())
           .andExpect(jsonPath("$.attackType", is("STATE")))
           .andExpect(jsonPath("$.startTime", is(attack1.getStartTime().toString())));
        mvc.perform(get("/attack/" + UUID2).contentType(APPLICATION_JSON))
           .andExpect(status().isOk())
           .andExpect(jsonPath("$.attackType", is("NETWORK")))
           .andExpect(jsonPath("$.startTime", is(attack2.getStartTime().toString())));
    }

    @Test
    public void getAttackQueue () throws Exception {
        Queue<Attack> attackQueue = new LinkedBlockingDeque<>();
        attackQueue.add(attack1);
        when(attackManager.getNewAttackQueue()).thenReturn(attackQueue);
        mvc.perform(get("/attack/queue").contentType(APPLICATION_JSON))
           .andExpect(status().isOk())
           .andExpect(jsonPath("$[0].attackType", is("STATE")))
           .andExpect(jsonPath("$[0].startTime", is(attack1.getStartTime().toString())));
    }

    @Test
    public void startAttacks () throws Exception {
        mvc.perform(post("/attack/start").contentType(APPLICATION_JSON)).andExpect(status().isOk());
        verify(attackManager, times(1)).startAttacks(true);
    }

    @Test
    public void attackContainerWithId () throws Exception {
        Long containerId = new Random().nextLong();
        mvc.perform(post("/attack/start/" + containerId)).andExpect(status().isOk());
        verify(attackManager, times(1)).attackContainerId(containerId);
    }
}