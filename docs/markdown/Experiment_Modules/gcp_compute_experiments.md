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


[GCP Compute SDK for Java]: https://github.com/googleapis/google-cloud-java
[Google Cloud Instance Metadata Server]: https://cloud.google.com/compute/docs/storing-retrieving-metadata

[Compute instances.list]: https://cloud.google.com/compute/docs/reference/rest/v1/instances/list
[Compute instances.simulateMaintenanceEvent]: https://cloud.google.com/compute/docs/reference/rest/v1/instances/simulateMaintenanceEvent
[Compute zoneOperations.get]: https://cloud.google.com/compute/docs/reference/rest/v1/zoneOperations/get