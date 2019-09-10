/*
 *    Copyright (c) 2019 Thales Group
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 *
 */

package com.thales.chaos.container;

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
     * @param clazz            A class that extends the Container class.
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
