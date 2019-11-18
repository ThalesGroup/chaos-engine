# User Defined Experiments

User Defined Experiments create the ability for the user to create a set of Experiments with specific criteria.

## Mechanism

Two API endpoints exist to support User Defined Experiments. The first endpoint provides the mechanism for actually creating a set of experiments given a criteria input. The second endpoint provides the necessary criteria to recreate any earlier experiment.

When creating an experiment, containers are targeted using their Aggregation Identifier, as defined by each Platform specification. The Aggregation Identifier **should** be grouping containers that are operating together to offer the same service, and should be functionally identical to one another.

## User Defined Experiment JSON Structure

The parameter for a User Defined Experiment is a single object with two variables. The **platformType** variable should be the name of the Platform you wish to experiment on. The **experimentCriteria** variable requires an object of **containerIdentifier**, **experimentMethods\[\]**, and *optional* **specificContainerTargets\[\]**, to identify the aggregate container group, the type of experiments to run, and any specific targets which may not be identical (i.e., a MongoDB Primary node).

### Example

``` java
{
    "platformType": "KubernetesPlatform",
    "experimentCriteria": [
        {
            "containerIdentifier": "ngnix",
            "experimentMethods": [
                "terminateProcess.sh"
            ],
            "specificContainerTargets": []
        },
        {
            "containerIdentifier": "mongo",
            "experimentMethods": [
                "cpuBurn.sh",
                "starveRandomNumberGenerator.sh"
            ],
            "specificContainerTargets": [
                "mongo-qw93ut"
            ]
        }
    ]
}
```

## Experiment Build Endpoint

The Experiment Structure object can be sent as the data stream of a *POST* request to **/experiment/build** to create the experiments. The server expects a **Content-Type** of **application/json**.

### cURL Equivalent

``` bash
curl -X POST \
  http://localhost:8080/experiment/build \
  -H 'Accept: application/json;charset=utf-8' \
  -H 'Content-Type: application/json' \
  -d '{
    "platformType": "KubernetesPlatform",
    "experimentCriteria": [
        {
            "containerIdentifier": "ngnix",
            "experimentMethods": [
                "terminateProcess.sh"
            ],
            "specificContainerTargets": []
        },
        {
            "containerIdentifier": "mongo",
            "experimentMethods": [
                "cpuBurn.sh",
                "starveRandomNumberGenerator.sh"
            ],
            "specificContainerTargets": [
                "mongo-qw93ut"
            ]
        }
    ]
}'
```

### Potential Error Responses

An HTTP 5xx error may be returned if the request is malformed, or if the experiments cannot be created for any number of reasons. A more specific error code will be returned as part of the response body.

## Experiment History Endpoint

The Experiment History endpoint of **/experiment/history**, when accessed with a GET request, will return a map of Timestamps to Defined Experiment Structures. The data from this map can be extracted and used on the *build* endpoint to easily recreate experiments.

### History cURL and Response

**Request**

``` java
curl -X GET \
  http://localhost:8080/experiment/history \
  -H 'Accept: application/json;charset=utf-8'
```

**Response**

``` java
{
    "2019-08-29T12:55:31.139495Z": {
        "platformType": "KubernetesPlatform",
        "experimentCriteria": [
            {
                "containerIdentifier": "ngnix",
                "experimentMethods": [
                    "terminateProcess.sh"
                ],
                "specificContainerTargets": []
            },
            {
                "containerIdentifier": "application-67d549b976",
                "experimentMethods": [
                    "cpuBurn.sh"
                ],
                "specificContainerTargets": []
            }
        ]
    }
}
```

## Specific Container Targets

By specifying specific instances in the **specificContainerTargets\[\]** field, it is possible to direct specific experiments to occur on specific instances of a group that are not necessarily equal. This is useful for any applications which may have a mechanism for electing a primary node, such as MongoDB. When crafting an experiment for these applications, it you need to specify a specific experiment occur on a specific instance, construct the array of **specificContainerTargets** with the unique name of the target, and put the **experimentMethod** for that container in the same position in its array. Any **experimentMethods** which are not associated with a **specificContainerTargets** are then randomly assigned to any leftover targets.

``` json
{
    "containerIdentifier": "mongo",
    "experimentMethods": [
        "cpuBurn.sh",
        "starveRandomNumberGenerator.sh"
    ],
    "specificContainerTargets": [
        "mongo-qw93ut"
    ]
}
```

## Limitations

There is not enough information recorded and stored to validate "how" identical two experiments with the same criteria will be. For example, if running an experiment on a Cloud Platform VM that operates as a node in a Kubernetes Cluster, the effect of the experiment may be dependent on what pods are currently being served by that node. For example, an experiment that starves **/dev/random** may create a visible effect if a specific pod heavily uses random number generator being located on that node at that time. If that pod is absent, or not being used for that function at that time, then the impact on the service as a whole will be different.

In addition, autoscaling may have changed the number of containers in a platform at different times. For example, a service under heavy load at specific times may have 20 Kubernetes Pods for a given microservice at a given time. At lower peak times, it may be down to only 5. User Defined Experiments automatically ensure that at least one container per aggregation identifier will not be experimented upon.

Finally, while the Specific Container Targets helps provide a way to ensure that experiments occur on specific containers that are *less equal* than others, the engine makes no attempt to hold a history of which specific containers it randomly generated an experiment on, as the specific containers are unlikely to exist after experiment ended. There is also no system by which the engine distinguishes how specific application instances are different from others. In order to make use of Specific Container Targets, the engineers operating Chaos Engine will need to discover on their own that a specific instance is *different* and needs to be specifically handled.
