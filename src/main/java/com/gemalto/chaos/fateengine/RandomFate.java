package com.gemalto.chaos.fateengine;

import com.gemalto.chaos.container.Container;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Random;

@Component
public class RandomFate implements FateEngine {


    @Value("@{proability:0.2f")
    private static float destructionProbability;

    private static boolean canDestroy(float destructionProbability) {
        Random r = new Random();
        return r.nextFloat() < destructionProbability;
    }

    @Override
    public boolean canDestroy(Container container) {
        return canDestroy(destructionProbability);
    }

}
