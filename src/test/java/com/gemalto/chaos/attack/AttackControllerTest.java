package com.gemalto.chaos.attack;

import com.gemalto.chaos.attack.enums.AttackType;
import com.gemalto.chaos.attack.impl.GenericContainerAttack;
import org.hamcrest.collection.IsIterableContainingInAnyOrder;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Queue;
import java.util.Random;
import java.util.concurrent.LinkedBlockingDeque;

import static java.util.UUID.randomUUID;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class AttackControllerTest {
    private AttackController attackController;
    @Mock
    private AttackManager attackManager;
    private Attack attack1;
    private Attack attack2;

    @Before
    public void setUp () {
        attackController = new AttackController(attackManager);
        attack1 = GenericContainerAttack.builder().withAttackType(AttackType.STATE).build();
        attack2 = GenericContainerAttack.builder().withAttackType(AttackType.NETWORK).build();
    }

    @Test
    public void getAttacks () {
        when(attackManager.getActiveAttacks()).thenReturn(new HashSet<>(Arrays.asList(attack1, attack2)));
        assertThat(attackController.getAttacks(), IsIterableContainingInAnyOrder.containsInAnyOrder(attack1, attack2));
    }

    @Test
    public void getAttackById () {
        String UUID1 = randomUUID().toString();
        String UUID2 = randomUUID().toString();
        when(attackManager.getAttackByUUID(UUID1)).thenReturn(attack1);
        when(attackManager.getAttackByUUID(UUID2)).thenReturn(attack2);
        assertEquals(attackController.getAttackById(UUID1), attack1);
        assertEquals(attackController.getAttackById(UUID2), attack2);
    }

    @Test
    public void getAttackQueue () {
        Queue<Attack> attackQueue = new LinkedBlockingDeque<>();
        attackQueue.add(attack1);
        when(attackManager.getNewAttackQueue()).thenReturn(attackQueue);
        assertEquals(attackController.getAttackQueue().peek(), attack1);
    }

    @Test
    public void startAttacks () {
        attackController.startAttacks();
        verify(attackManager, times(1)).startAttacks();
    }

    @Test
    public void attackContainerWithId () {
        Long containerId = new Random().nextLong();
        attackController.attackContainerWithId(containerId);
        verify(attackManager, times(1)).attackContainerId(containerId);
    }
}