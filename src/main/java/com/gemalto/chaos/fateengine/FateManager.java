package com.gemalto.chaos.fateengine;

import com.gemalto.chaos.container.Container;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class FateManager {
    @Autowired
    private List<FateEngine> fateEngines;
    private List<FateEngine> weightedFateEngines;
    private HashMap<Container, AbstractMap.SimpleEntry<FateEngine, Integer>> fateEngineMap = new HashMap<>();

    public FateEngine getFateEngineForContainer (Container container) {
        if (fateEngineMap.containsKey(container)) {
            return getAndDecrementTTL(container);
        } else {
            return generateFateEngineForContainer(container);
        }
    }

    private FateEngine getAndDecrementTTL (Container container) {
        AbstractMap.SimpleEntry<FateEngine, Integer> fateEngineWithTTL = fateEngineMap.get(container);
        Integer timeToLive = fateEngineWithTTL.getValue();
        if (timeToLive > 1) {
            fateEngineMap.put(container, new AbstractMap.SimpleEntry<>(fateEngineWithTTL.getKey(), timeToLive - 1));
        } else {
            fateEngineMap.remove(container);
        }
        return fateEngineWithTTL.getKey();
    }

    private FateEngine generateFateEngineForContainer (Container container) {
        FateEngine chosenFateEngine = generateFateEngine();
        Integer timeToLive = generateTimeToLive(chosenFateEngine);
        fateEngineMap.put(container, new AbstractMap.SimpleEntry<>(chosenFateEngine, timeToLive));
        return chosenFateEngine;
    }

    private FateEngine generateFateEngine () {
        // TODO : This can probably be improved upon into a more elegant solution.
        if (weightedFateEngines == null) {
            weightedFateEngines = new ArrayList<>();
            for (FateEngine fateEngine : fateEngines) {
                for (int i = 0; i < fateEngine.getFateWeight(); i++) {
                    weightedFateEngines.add(fateEngine);
                }
            }
        }
        Collections.shuffle(weightedFateEngines);
        return weightedFateEngines.get(0);
    }

    private Integer generateTimeToLive (FateEngine fateEngine) {
        Integer maxTimeToLive = fateEngine.getMaxTimeToLive();
        Integer minTimeToLive = fateEngine.getMinTimeToLive();
        return new Random().nextInt(maxTimeToLive - minTimeToLive) + minTimeToLive;
    }
}
