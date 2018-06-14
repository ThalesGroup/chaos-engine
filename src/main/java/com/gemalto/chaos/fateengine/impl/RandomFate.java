package com.gemalto.chaos.fateengine.impl;

import com.gemalto.chaos.fateengine.FateEngine;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Random;

@Component
public class RandomFate extends FateEngine {

    RandomFate(@Value("${probability:0.2}") float destructionProbability) {
        this.destructionProbability = destructionProbability;
        minTimeToLive = 3;
        maxTimeToLive = 10;
        fateWeight = 10;
    }

    private float destructionProbability;

    private static boolean canDestroy(float destructionProbability, Random random) {
        return random.nextFloat() < destructionProbability;
    }

    @Override
    public boolean canDestroy() {
        return canDestroy(destructionProbability, new Random());
    }

}
