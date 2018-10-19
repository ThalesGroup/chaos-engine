package com.gemalto.chaos.container;

import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.HashSet;
import java.util.Optional;

@Component
public class ContainerManager {
    private final Collection<Container> containers = new HashSet<>();

    /**
     * Returns an existing container of the specified class with the specified unique identifier (as validated by the
     * Container's compareUniqueIdentifier method).
     *
     * @param clazz            A class that extends the com.gemalto.chaos.container.Container class.
     * @param uniqueIdentifier The expected unique identifier for the container, as evaluated by Container::compareUniqueIdentifier
     * @return Either an existing Container object, or null if no container exists.
     */
    public <T extends Container> T getMatchingContainer (Class<T> clazz, String uniqueIdentifier) {
        Optional<T> match;
        synchronized (containers) {
            match = containers.stream()
                              .filter(clazz::isInstance)
                              .map(clazz::cast)
                              .filter(container -> container.compareUniqueIdentifier(uniqueIdentifier))
                              .findFirst();
        }
        return match.orElse(null);
    }

    /**
     * Adds a container to the cache used by getMatchingContainer.
     * @param container A Container object.
     */
    public void offer (Container container) {
        synchronized (containers) {
            containers.add(container);
        }
    }

}
