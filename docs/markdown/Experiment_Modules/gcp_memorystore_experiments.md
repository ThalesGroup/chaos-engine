# Google Cloud Memorystore Experiments

## SDK

The GCP Memorystore module uses the Official [GCP Memorystore SDK for Java] from Google ([JavaDoc]).

### Version

All Google SDKs are included via the Google Cloud `libraries-bom` Maven package. The current version of the package is 3.0.0.

## Configuration

| Key Name | Description | Default | Mandatory |
| --- | --- | :---: | :---: |
| `gcp.memorystore` | The presence of this key enables the module. | N/A | Yes |
| `gcp.json-key` | This key should be the JSON Key of the Service Account the module is to use. | N/A | Yes |
| `gcp.project-id` | This key controls which GCP Project the module will experiment on. | N/A | Yes |
| `gcp.memorystore.include-filter.<label-key-name>` | Used for filtering the inclusion of GCP Memorystore Instances based on the presence of a specific key/value pair of label. See [Filtering](#filtering) for more information. | N/A | No |
| `gcp.memorystore.exclude-filter.<label-key-name>` | Used for filtering the inclusion of GCP Memorystore Instances based on the presence of a specific key/value pair of label. See [Filtering](#filtering) for more information. | N/A | No |

### Required Permissions

Each experiment below lists the specific API calls it makes. These API calls map 1-to-1 with individual IAM permissions. 

If you do not wish to manage maintaining a role for Chaos Engine, the `roles.editor` role can be used instead, but be aware that this role contains many powerful permissions that are unnecessary for the Chaos Engine to operate.

## Node Discovery

Nodes are discovered using the [Memorystore instances.list] API. The results are parsed in all regions and converted into Java objects.

!!! note Standard Tier Instances Required
    Memorystore experiments can be executed on **STANDARD Tier** instances only. Other types are filtered.

### Filtering

The GCP Memorystore platform supports both inclusive and exclusive filtering based on instance metadata key/value pairs.
If any include-filters are specified, all must exist in the metadata of the instance.
Similarly, if any exclude-filters are specified, none must exist in the metadata of the instance.

The filter values are case-sensitive.

## Experiment Methods

### Failover 

Initiates a failover of the master node to current replica node for a specific STANDARD tier Cloud Memorystore instance.

#### Mechanism

API: [Memorystore instances.failover]

The `failover` API is called against the Instance. The operation is performed with  **LIMITED_DATA_LOSS** [data protection mode] **on**.
##### Health Check

API: [Memorystore operations.get]

The failover operation status is queried and checked. The experiment is considered finished when the `done` field in [ListOperationsResponse] is true.

##### Self Healing

Because this is an entirely cloud managed operation, Self Healing is not possible. Once the operation has been started, it cannot be stopped. 

### Forced Failover 

Initiates a forced failover of the master node to current replica node for a specific STANDARD tier Cloud Memorystore instance.

#### Mechanism

API: [Memorystore instances.failover]

The `failover` API is called against the Instance. The operation is performed with  **FORCE_DATA_LOSS** [data protection mode] **on**.
##### Health Check

API: [Memorystore operations.get]

The failover operation status is queried and checked. The experiment is considered finished when the `done` field in [ListOperationsResponse] is true.

##### Self Healing

Because this is an entirely cloud managed operation, Self Healing is not possible. Once the operation has been started, it cannot be stopped. 



[GCP Memorystore SDK for Java]: https://github.com/googleapis/java-redis
[JavaDoc]: https://googleapis.dev/java/google-cloud-clients/latest/index.html
[Memorystore instances.list]: https://cloud.google.com/memorystore/docs/redis/reference/rest/v1/projects.locations.instances/list
[Memorystore operations.get]: https://cloud.google.com/memorystore/docs/redis/reference/rest/v1/projects.locations.operations/get
[ListOperationsResponse]:https://cloud.google.com/memorystore/docs/redis/reference/rest/Shared.Types/ListOperationsResponse#Operation
[Memorystore instances.failover]: https://cloud.google.com/memorystore/docs/redis/reference/rest/v1/projects.locations.instances/failover
[Data protection mode]: https://cloud.google.com/memorystore/docs/redis/reference/rest/v1/projects.locations.instances/failover#DataProtectionMode