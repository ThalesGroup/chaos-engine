package com.gemalto.chaos.fateengine;

import com.gemalto.chaos.container.Container;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Random;

@Component
public class RandomFate implements FateEngine {

    private static float destructionProbability;

    @Value("@{probability:0.2f")
    public void setDestructionProbability(float probability) {
        destructionProbability = probability;
    }


    private static boolean canDestroy(float destructionProbability) {
        Random r = new Random();
        return r.nextFloat() < destructionProbability;
    }

    @Override
    public boolean canDestroy(Container container) {
        return canDestroy(destructionProbability);
    }

}
