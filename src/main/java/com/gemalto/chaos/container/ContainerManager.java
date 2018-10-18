package com.gemalto.chaos.container;

import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.HashSet;
import java.util.Optional;

@Component
public class ContainerManager {
    private final Collection<Container> containers = new HashSet<>();

    public <T extends Container> T getMatchingContainer (Class<T> clazz, String uniqueIdentifier) {
        synchronized (containers) {
            Optional<T> match = containers.stream()
                                          .filter(clazz::isInstance)
                                          .map(clazz::cast)
                                          .filter(container -> container.compareUniqueIdentifier(uniqueIdentifier))
                                          .findFirst();
            return match.orElse(null);
        }
    }

    public void offer (Container container) {
        synchronized (containers) {
            containers.add(container);
        }
    }

}
