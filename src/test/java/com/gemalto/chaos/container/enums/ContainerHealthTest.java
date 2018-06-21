package com.gemalto.chaos.container.enums;

import org.junit.Test;

import static org.junit.Assert.assertNotEquals;

public class ContainerHealthTest {
    @Test
    public void ContainerHealthTestImpl () {
        assertNotEquals(ContainerHealth.valueOf("NORMAL"), null);
        assertNotEquals(ContainerHealth.valueOf("UNDER_ATTACK"), null);
    }
}