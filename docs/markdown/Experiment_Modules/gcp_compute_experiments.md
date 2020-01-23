# Google Cloud Compute Engine Experiments

## SDK

The GCP Compute Engine module uses the Official [GCP Compute SDK for Java] from Google.

### Version

All Google SDKs are included via the Google Cloud `libraries-bom` Maven package. The current version of the package is 3.0.0.

## Configuration

| Key Name | Description | Default |
| --- | --- | :---: |
| `gcp.compute` | The presence of this key enables the module | N/A |
| `gcp.compute.project-id` | This key controls which GCP Project the module will experiment on | N/A |
| `gcp.compute.json-key` | This key should be the JSON Key of the Service Account the module is to use | N/A |
| `gcp.compute.include-filter.<metadata-key-name>` | Used for filtering the inclusion of GCP Compute Engine Instances based on the presence of a specific key/value pair of Metadata. See [#Filtering] for more information. | N/A |
| `gcp.compute.exclude-filter.<metadata-key-name` | Used for filtering the inclusion of GCP Compute Engine Instances based on the presence of a specific key/value pair of Metadata. See [#Filtering] for more information. | N/A |



### Credential Sharing Across GCP Modules

Enabled GCP Modules can share credentials amongst each other. If the specific module does not have any credentials configured, it will attempt to use credentials from another enabled module.
If multiple other modules hold credentials, there is no specific preference for which module it will inherit. It is recommended to provide credentials for either all modules, or exactly one module.

If credentials are supplied for a specific GCP Module, that module will always use its specifically configured credentials.




[GCP Compute SDK for Java]: https://github.com/googleapis/google-cloud-java