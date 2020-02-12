# Google Cloud Compute Engine Experiments

## SDK

The GCP Compute Engine module uses the Official [GCP Compute SDK for Java] from Google.

### Version

All Google SDKs are included via the Google Cloud `libraries-bom` Maven package. The current version of the package is 3.0.0.

## Configuration

| Key Name | Description | Default |
| --- | --- | :---: |
| `gcp.compute` | The presence of this key enables the module. | N/A |
| `gcp.compute.project-id` | This key controls which GCP Project the module will experiment on. | N/A |
| `gcp.compute.json-key` | This key should be the JSON Key of the Service Account the module is to use. | N/A |
| `gcp.compute.include-filter.<metadata-key-name>` | Used for filtering the inclusion of GCP Compute Engine Instances based on the presence of a specific key/value pair of Metadata. See [Filtering](#filtering) for more information. | N/A |
| `gcp.compute.exclude-filter.<metadata-key-name>` | Used for filtering the inclusion of GCP Compute Engine Instances based on the presence of a specific key/value pair of Metadata. See [Filtering](#filtering) for more information. | N/A |



!!! note "Credential Sharing Across GCP Modules"
    Enabled GCP Modules can share credentials amongst each other. If the specific module does not have any credentials configured, it will attempt to use credentials from another enabled module.
    If multiple other modules hold credentials, there is no specific preference for which module it will inherit. It is recommended to provide credentials for either all modules, or exactly one module.
    
    If credentials are supplied for a specific GCP Module, that module will always use its specifically configured credentials.

## Node Discovery

Nodes are discovered using the [Compute instances.list] API. The results are parsed in all zones and converted into Java objects.

### Filtering

The GCP Compute platform supports both inclusive and exclusive filtering based on instance metadata key/value pairs.
If any include-filters are specified, all must exist in the metadata of the instance.
Similarly, if any exclude-filters are specified, none must exist in the metadata of the instance.

The filter values are case-sensitive.

### Self Awareness

The GCP Compute platform uses the [Google Cloud Instance Metadata Server] to discover its own Google Cloud Resource ID.
Instances are evaluated against that resource ID and removed from the pool of potential experiments. 


## Experiment Methods

### Simulate Maintenance Event

Google Cloud Compute regularly performs [Maintenance Events](https://cloud.google.com/compute/docs/instances/setting-instance-scheduling-options#maintenanceevents) on their physical hosts, for operations such as kernel upgrades, or hardware maintenance. When they perform these tasks, they take actions on all Virtual Machines on that host. A Compute Engine VM may be live-migrated to another host, or it may be terminated and recreated (requires configuration). This experiment tests to validate that no unforeseen problems occur as a result of these maintenance events, which may happen at any time. 

#### Mechanism

API: [Compute instances.simulateMaintenanceEvent]

The `simulateMaintenanceEvent` API is called against the Instance UUID. This operation performs the real action of either live-migrating or replacing the VM.

##### Health Check

API: [Compute zoneOperations.get]

The operation status is called and checked. The experiment is considered finished when the operation returns Progress >= 100.

##### Self Healing

Because this is an entirely cloud managed operation, Self Healing is not possible. Once the operation has been started, it cannot be stopped. 

### Stop Instance

Instances that are not part of instance groups can be stopped. A VM can be configured to be automatically restarted if it is in an unexpected stopped state, but this takes time to recognize and accomplish. 

#### Mechanism 

API: [Compute instances.stop]

The `stop` API is called against the instance. 

#### Health Check

API: [Compute zoneOperations.get], [Compute instances.get]

The operation for the `stop` is polled until complete. Then, the instance is specifically called using the `get` API, and the `status` field compared. If the `status` is not "RUNNING", then the experiment is still in progress. If the `get` call returns an HTTP 404, the instance no longer exists and the experiment is considered failed.

#### Self Healing

API: [Compute instances.start]

The `start` API is called to self-heal the instance after the experiment duration.

### Reset Instance

Instances that are not part of instance groups can be reset. The VM is restarted, and temporarily unavailable. If the startup sequence is extensive, it may result in full application stack issues.

#### Mechanism 

API: [Compute instances.reset]

The `reset` API is called against the instance. 

#### Health Check

API: [Compute zoneOperations.get], [Compute instances.get]

The operation for the `stop` is polled until complete. Then, the instance is specifically called using the `get` API, and the `status` field compared. If the `status` is not "RUNNING", then the experiment is still in progress. If the `get` call returns an HTTP 404, the instance no longer exists and the experiment is considered failed.

#### Self Healing

API: [Compute instances.start]

The `start` API is called to self-heal the instance after the experiment duration.


### Recreate Instance in Instance Group

Recreating an instance that is part of an instance group replaces and reinitializes a VM. This operation is similar to how Google Cloud will heal an instance that is failing health checks. This experiment may find errors in how an instance group behaves when it is below capacity by one instance, or issues with rolling out an old image after an update in [opportunistic mode](https://cloud.google.com/compute/docs/instance-groups/rolling-out-updates-to-managed-instance-groups#starting_an_opportunistic_or_proactive_update) has started.

#### Mechanism

API: [Compute instanceGroupManagers.recreateInstances]

The `recreateInstances` API is called against the managed instance group, passing the specific instance as a parameter. 

#### Health Check

API: [Compute zoneOperations.get], [Compute instanceGroup.get], [Compute instanceGroupManager.get], [Compute regionInstanceGroup.get], [Compute regionInstanceGroupManager.get]

The operation status is called and checked. If the operation is completed, additionally the instance group size and target size are called via the (region)InstanceGroup and associated Manager API's. If the target and actual size are equal, then the instance group manager has properly resolved the capacity.

#### Self Healing

Because this is an entirely cloud managed operation, Self Healing is not possible. Once the operation has been started, it cannot be stopped. 

#### Finalization

After the experiment is finished, Chaos Engine will perform a [Compute instances.get] operation against the specific instance name it experimented upon. While most other metadata will remain the same, the new version of the instance will have a new unique identifier that needs to be updated locally for future experiments. 

### Remove Firewall Tags from Instance

Removing firewall tags from an instance will block the flow of traffic into the instance. This can simulate a network availability issue for the one specific instance. This experiment is only applicable to instances that are not part of instance groups.

#### Mechanism

API: [Compute instances.get], [Compute instances.setTags]

The latest tags and their fingerprint are fetched using the [Compute instances.get] API. The fingerprint is used along with an empty set of tags in the [Compute instances.setTags] API.

#### Health Check

API: [Compute zoneOperations.get], [Compute instances.get]

The operation status from the original `setTags` operation is checked. If the operation is completed, the instance data is retrieved and the contents of the tags are compared. If the original tags that were fetched during the experiment startup match the current tags, the experiment is considered complete. Tag order is irrelevant in this comparison.

#### Self Healing

API: [Compute instances.get], [Compute instances.setTags]

The new tag fingerprint is retrieved using [Compute instances.get], and the original tags from setup are pushed back using [Compute instances.setTags]. 


[GCP Compute SDK for Java]: https://github.com/googleapis/google-cloud-java
[Google Cloud Instance Metadata Server]: https://cloud.google.com/compute/docs/storing-retrieving-metadata


[Compute instanceGroup.get]: https://cloud.google.com/compute/docs/reference/rest/v1/instanceGroups/get
[Compute instanceGroupManager.get]: https://cloud.google.com/compute/docs/reference/rest/v1/instanceGroupManagers/get
[Compute instanceGroupManagers.recreateInstances]: https://cloud.google.com/compute/docs/reference/rest/v1/instanceGroupManagers/recreateInstances
[Compute instances.get]: https://cloud.google.com/compute/docs/reference/rest/v1/instances/get
[Compute instances.list]: https://cloud.google.com/compute/docs/reference/rest/v1/instances/list
[Compute instances.reset]: https://cloud.google.com/compute/docs/reference/rest/v1/instances/reset
[Compute instances.setTags]: https://cloud.google.com/compute/docs/reference/rest/v1/instances/setTags
[Compute instances.simulateMaintenanceEvent]: https://cloud.google.com/compute/docs/reference/rest/v1/instances/simulateMaintenanceEvent
[Compute instances.start]: https://cloud.google.com/compute/docs/reference/rest/v1/instances/start
[Compute instances.stop]: https://cloud.google.com/compute/docs/reference/rest/v1/instances/stop
[Compute regionInstanceGroup.get]: https://cloud.google.com/compute/docs/reference/rest/v1/regionInstanceGroups/get
[Compute regionInstanceGroupManager.get]: https://cloud.google.com/compute/docs/reference/rest/v1/regionInstanceGroupManagers/get
[Compute zoneOperations.get]: https://cloud.google.com/compute/docs/reference/rest/v1/zoneOperations/get