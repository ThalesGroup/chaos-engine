package com.gemalto.chaos.fateengine.impl;

import com.gemalto.chaos.fateengine.FateEngine;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.Random;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

@RunWith(MockitoJUnitRunner.class)
public class RandomFateTest {

    private FateEngine fateEngine;

    @Mock
    private Random random;

    @Test
    public void canDestroy() {
        fateEngine = new RandomFate(0.2f, random);
        Mockito.when(random.nextFloat())
                .thenReturn(0.2f)
                .thenReturn(0.1999f)
                .thenReturn(0.0f)
                .thenReturn(0.21f)
                .thenReturn(1.0f);
        assertFalse(fateEngine.canDestroy());

        assertTrue(fateEngine.canDestroy());
        assertTrue(fateEngine.canDestroy());


        assertFalse(fateEngine.canDestroy());
        assertFalse(fateEngine.canDestroy());


    }
}