package com.gemalto.chaos.fateengine.impl;

import com.gemalto.chaos.fateengine.FateEngine;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Random;

@Component
public class RandomFate extends FateEngine {

    @Value("${probability:0.2}")
    private double destructionProbability;
    private Random random;

    @Autowired
    RandomFate() {
        minTimeToLive = 3;
        maxTimeToLive = 10;
        fateWeight = 10;
        random = new Random();
    }

    RandomFate(double destructionProbability, Random random) {
        this.destructionProbability = destructionProbability;
        this.random = random;
    }

    private static boolean canDestroy(double destructionProbability, Random random) {
        return random.nextDouble() < destructionProbability;
    }

    @Override
    public boolean canDestroy() {
        return canDestroy(destructionProbability, random);
    }

}
