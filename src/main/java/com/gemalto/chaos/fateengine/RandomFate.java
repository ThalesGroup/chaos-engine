package com.gemalto.chaos.fateengine;

import com.gemalto.chaos.container.Container;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Random;

@Component
public class RandomFate implements FateEngine {

    RandomFate(@Value("${probability:0.2}") float destructionProbability) {
        this.destructionProbability = destructionProbability;
    }

    private float destructionProbability;

    private static boolean canDestroy(float destructionProbability, Random random) {
        return random.nextFloat() < destructionProbability;
    }

    @Override
    public boolean canDestroy(Container container) {
        return canDestroy(destructionProbability, new Random());
    }

}
