# Container Manager

## Description

The Container Manager in Chaos Engine is designed to cache information about containers and prevent the need for recreating them repeatedly. Since we expect most containers to have a relatively long life cycle, we should prevent recreating objects every time we scan the platforms.

## Development Notes

### Usage

See this simple code example on how the Container Manager should be used in the process of creating a container.

**ContainerManager Usage**

```java
package com.thales.chaos;

import com.thales.chaos.container.Container;
import com.thales.chaos.container.ContainerManager;
import com.thales.chaos.container.enums.ContainerHealth;
import com.thales.chaos.experiment.enums.ExperimentType;
import com.thales.chaos.notification.datadog.DataDogIdentifier;
import com.thales.chaos.platform.Platform;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.validation.constraints.NotNull;

@Service
public class Example {
    @Autowired
    private ContainerManager containerManager;

    private ExampleContainer getContainer (Object ... args) {
        ExampleContainer exampleContainer = containerManager.getMatchingContainer(ExampleContainer.class, args.getIdentifier());
        if (exampleContainer == null) {
            exampleContainer = actuallyCreateContainer();
            containerManager.offer(exampleContainer);
        }
        return exampleContainer;
    }

    private ExampleContainer actuallyCreateContainer()
    {
        return new ExampleContainer() {};
    }

    private abstract class ExampleContainer extends Container {
    }
}
```

In this example class, a Spring Service is attempting to look for an existing container if it already exists in the Container Manager cache ***(Line 20)***. If it does not exist ***(21)***, it will create the container using another call ***(22)***, and then offer that container back into the Container Manager ***(23)***, before finally returning the container ***(25)*** that was either retrieved from the cache or created.
