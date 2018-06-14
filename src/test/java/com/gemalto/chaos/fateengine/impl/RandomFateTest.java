package com.gemalto.chaos.fateengine.impl;

import com.gemalto.chaos.fateengine.FateEngine;
import org.junit.Assert;
import org.junit.Test;

public class RandomFateTest {

    private FateEngine fateEngine;

    @Test
    public void canDestroy() {
        fateEngine = new RandomFate(0);
        boolean firstFate = fateEngine.canDestroy();
        fateEngine = new RandomFate(1);
        boolean secondFate = fateEngine.canDestroy();
        Assert.assertNotEquals(firstFate, secondFate);
    }
}