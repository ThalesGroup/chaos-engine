package com.gemalto.chaos.attack.enums;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.Random;

import static org.junit.Assert.assertNotEquals;

@RunWith(MockitoJUnitRunner.class)
public class AttackTypeTest {
    @Mock
    private Random random;

    @Test
    public void AttackTypeTestImpl () {
        assertNotEquals(AttackType.valueOf("NETWORK"), null);
        assertNotEquals(AttackType.valueOf("RESOURCE"), null);
        assertNotEquals(AttackType.valueOf("STATE"), null);
    }
}