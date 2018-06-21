package com.gemalto.chaos.attack.enums;

import org.junit.Test;

import static org.junit.Assert.assertNotEquals;

public class AttackStateTest {
    @Test
    public void AttackStateTestImpl () {
        assertNotEquals(AttackState.valueOf("FINISHED"), null);
        assertNotEquals(AttackState.valueOf("STARTED"), null);
        assertNotEquals(AttackState.valueOf("NOT_YET_STARTED"), null);
    }
}