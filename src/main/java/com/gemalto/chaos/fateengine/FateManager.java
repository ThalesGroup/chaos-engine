package com.gemalto.chaos.fateengine;

import com.gemalto.chaos.container.Container;
import javafx.util.Pair;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Random;

@Component
public class FateManager {

    @Autowired
    private List<FateEngine> fateEngines;

    private HashMap<Container, Pair<FateEngine, Integer>> fateEngineMap = new HashMap<>();

    FateEngine getFateEngineForContainer(Container container) {
        if (fateEngineMap.containsKey(container)) {
            return getAndDecrementTTL(container);
        } else {
            return generateFateEngineForContainer(container);
        }
    }

    private FateEngine getAndDecrementTTL(Container container) {
        Pair<FateEngine, Integer> fateEngineWithTTL = fateEngineMap.get(container);
        Integer timeToLive = fateEngineWithTTL.getValue();
        if (timeToLive > 1) {
            fateEngineMap.put(container,
                    new Pair<>(fateEngineWithTTL.getKey(), timeToLive - 1));
        } else {
            fateEngineMap.remove(container);
        }
        return fateEngineWithTTL.getKey();
    }

    private FateEngine generateFateEngineForContainer(Container container) {
        return generateFateengineForContainerWithTTL(container, 1, 5);
    }

    private FateEngine generateFateengineForContainerWithTTL(Container container, Integer minTimetoLive, Integer maxTimeToLive) {
        Collections.shuffle(fateEngines);
        FateEngine chosenFateEngine = fateEngines.get(0);

        Integer timetoLive = new Random().nextInt(maxTimeToLive - minTimetoLive) + minTimetoLive;

        fateEngineMap.put(
                container,
                new Pair<>(chosenFateEngine, timetoLive)
        );

        return chosenFateEngine;
    }

}
