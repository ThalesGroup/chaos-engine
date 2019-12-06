# Coding Conventions

These coding conventions ensure a consistent style across the Chaos Engine, and avoid potential issues with the large number of Third Party Libraries that are used for interacting with each Cloud Technology.

## Project Structure

The core project structure is divided into multiple packages. The Services package should define Beans to be used by the Platform layer. The platform layer should act as a wrapper for those services. The Container layer is created by the platform layer, and should not interact directly with the Service layer. As another way of saying that, the Container layer should be agnostic to the specific libraries used for communicating with the actual platform. A change in libraries should only require changes in the Platform layer.

## Chain Calls

In General, the Law of Demeter should apply to all packages in Chaos Engine. Chain calls that go through multiple packages can create a problem understanding where information comes from, and where it should be filtered or acted upon. A chain call should never pass through multiple packages.

When considering Third Party Libraries, chain calls are unavoidable. However, in order to prevent Null Pointer Exceptions from occurring due to potential faults in the libraries, Java Optionals should be used, with chained **map** calls to go through the object layers. This ensures that potential Null values are parsed within and can be handled by using **.orElse\*** methods of the Optional class.


```java tab="Bad Chain Call"
public ContainerHealth checkHealth (KubernetesPodContainer container) {
 V1Pod pod = getPodFromContainer(container);
 return pod.getStatus()
 .getContainerStatuses()
 .stream()
 .anyMatch(s -> !s.isReady()) ? ContainerHealth.RUNNING_EXPERIMENT : ContainerHealth.NORMAL)
}

/* In this example, there are multiple opportunities
 for a Null entry to cause a Null Pointer Exception. */
```



```java tab="Good Chain Call"
public ContainerHealth checkHealth (KubernetesPodContainer kubernetesPodContainer) {
    V1Pod result = getPodFromContainer(container);
    return Optional.ofNullable(result)
               .map(V1Pod::getStatus)
               .map(V1PodStatus::getContainerStatuses)
               .map(Collection::stream)
               .map(s -> s.anyMatch(ContainerStatus::isReady))
               .map(aBoolean -> aBoolean ? ContainerHealth.NORMAL : ContainerHealth.RUNNING_EXPERIMENT)
               .orElse(ContainerHealth.DOES_NOT_EXIST);
}

/* In this example, the Optional class wraps the method
 calls, and uses an orElse value to provide a third potential
 output from this method (essentially mapping True, False, 
 and Null) */

```

