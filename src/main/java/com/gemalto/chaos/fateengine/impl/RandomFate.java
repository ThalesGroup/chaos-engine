package com.gemalto.chaos.fateengine.impl;

import com.gemalto.chaos.fateengine.FateEngine;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Random;

@Component
public class RandomFate extends FateEngine {

    @Value("{probability:0.2}")
    private float destructionProbability;
    private Random random;

    @Autowired
    RandomFate() {
        minTimeToLive = 3;
        maxTimeToLive = 10;
        fateWeight = 10;
        random = new Random();
    }

    RandomFate(float destructionProbability, Random random) {
        this.destructionProbability = destructionProbability;
        this.random = random;
    }

    private static boolean canDestroy(float destructionProbability, Random random) {
        return random.nextFloat() < destructionProbability;
    }

    @Override
    public boolean canDestroy() {
        return canDestroy(destructionProbability, random);
    }

}
