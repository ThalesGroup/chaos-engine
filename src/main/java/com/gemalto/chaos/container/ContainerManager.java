package com.gemalto.chaos.container;

import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Optional;

@Component
public class ContainerManager {
    private final Collection<Container> containers = Collections.synchronizedCollection(new HashSet<>());

    public <T extends Container> T getMatchingContainer (Class<T> clazz, String uniqueIdentifier) {
        Optional<T> match = containers.stream()
                                      .filter(clazz::isInstance)
                                      .map(clazz::cast)
                                      .filter(container -> container.compareUniqueIdentifier(uniqueIdentifier))
                                      .findFirst();
            return match.orElse(null);
    }

    public void offer (Container container) {
        containers.add(container);
    }

}
