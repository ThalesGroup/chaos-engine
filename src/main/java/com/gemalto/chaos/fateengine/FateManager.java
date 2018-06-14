package com.gemalto.chaos.fateengine;

import com.gemalto.chaos.container.Container;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class FateManager {

    @Autowired
    private List<FateEngine> fateEngines;

    private HashMap<Container, AbstractMap.SimpleEntry<FateEngine, Integer>> fateEngineMap = new HashMap<>();

    public FateEngine getFateEngineForContainer(Container container) {
        if (fateEngineMap.containsKey(container)) {
            return getAndDecrementTTL(container);
        } else {
            return generateFateEngineForContainer(container);
        }
    }

    private FateEngine getAndDecrementTTL(Container container) {
        AbstractMap.SimpleEntry<FateEngine, Integer> fateEngineWithTTL = fateEngineMap.get(container);
        Integer timeToLive = fateEngineWithTTL.getValue();
        if (timeToLive > 1) {
            fateEngineMap.put(container,
                    new AbstractMap.SimpleEntry<>(fateEngineWithTTL.getKey(), timeToLive - 1));
        } else {
            fateEngineMap.remove(container);
        }
        return fateEngineWithTTL.getKey();
    }

    private FateEngine generateFateEngineForContainer(Container container) {
        Collections.shuffle(fateEngines);
        FateEngine chosenFateEngine = fateEngines.get(0);

        Integer timeToLive = generateTimeToLive(chosenFateEngine);

        fateEngineMap.put(
                container,
                new AbstractMap.SimpleEntry<>(chosenFateEngine, timeToLive)
        );

        return chosenFateEngine;
    }

    private Integer generateTimeToLive(FateEngine fateEngine) {
        Integer maxTimeToLive = fateEngine.getMaxTimeToLive();
        Integer minTimeToLive = fateEngine.getMinTimeToLive();

        return new Random().nextInt(maxTimeToLive - minTimeToLive) + minTimeToLive;
    }

}
