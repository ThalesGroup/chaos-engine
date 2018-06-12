package com.gemalto.chaos.fateengine;

import org.junit.Assert;
import org.junit.Test;

public class RandomFateTest {

    private FateEngine fateEngine;

    @Test
    public void canDestroy() {
        fateEngine = new RandomFate(0);
        boolean firstFate = fateEngine.canDestroy(null);
        fateEngine = new RandomFate(1);
        boolean secondFate = fateEngine.canDestroy(null);
        Assert.assertNotEquals(firstFate, secondFate);
    }
}