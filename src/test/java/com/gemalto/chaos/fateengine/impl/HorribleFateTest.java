package com.gemalto.chaos.fateengine.impl;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertTrue;

public class HorribleFateTest {
    private HorribleFate horribleFate;

    @Before
    public void setUp () {
        horribleFate = new HorribleFate();
    }

    @Test
    public void canDestroy () {
        assertTrue(horribleFate.canDestroy());
    }
}