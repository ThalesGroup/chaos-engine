# Chaos Engine

The Chaos Engine is designed to intermittently destroy or degrade instances of applications running in cloud based infrastructure. These events are designed to occur while the appropriate resources are available to resolve the issue if the platform fails to do so on it's own.

This is designed to simulate real cloud based outages that could occur. The code running on the cloud infrastructure should be robust enough to automatically recover. If it is, then the chaos event is a valid proof of concept. If it fails to, then it exposes an issue that could have happened at a less opportunistic time.

## Chaos Types
The following platforms act as entry points for the Chaos Engine. At least one needs to be configured.

### Cloud Foundry

#### Supports
| Chaos Type | Supported | Description |
| :-: | :-: | --- |
| STATE | Yes | Supports randomly stopping application instances. |
| RESOURCE | No  | Does not support starving of CPU, Memory, or Disk Performance |
| NETWORK | No  | Does not support interfering in network latency, corruption, or reliability |

#### Variables
| Variable | Description | Default |
|---|---|---|
| *`cf_apihost`* | Cloud Foundry API URI | \<None\> |
| `cf_port` | Cloud Foundry API Port | `443` |
| `cf_username` | Cloud Foundry Username | \<None\> |
| `cf_password` | Cloud Foundry Password | \<None\> |
| `cf_organization` | Cloud Foundry Organization | \<None\> |
| `cf_space` | Cloud Foundry Space | `default` |
| `cf_linked_applications` | Dependent Cloud Foundry applications. These are ignored entirely from Chaos, so as to prevent accidentally killing a dependent service | \<None\> |

### Amazon AWS EC2

#### Supports
| Chaos Type | Supported | Description |
| :-: | :-: | --- |
| STATE | Yes | Supports randomly stopping EC2 instances. |
| RESOURCE | No | Does not support starving of CPU, Memory, or Disk Resources
| NETWORK | No | Does not support interfering in network latency, corruption, reliability, or routing. |

#### Variables
| Variable | Description | Default |
| --- | --- | ---
| *`AWS_ACCESS_KEY_ID`*     | AWS Access Key ID | \<None\> |
| *`AWS_SECRET_ACCESS_KEY`* | AWS Secret Access Key | \<None\> |
| `AWS_REGION` | AWS Region to target | `us-east-2` |
| `AWS_FILTER_KEYS` | Comma separated list of keys to use for filtering AWS EC2 Container | \<None\> |
| `AWS_FILTER_VALUES` | Comma separated list of values to use for filtering AWS EC2 Container.  | \<None\> |


## Scheduling

The Chaos Engine is designed to run persistently, but only perform events on a certain schedule, and with exceptions. Here is how to control when chaos will occur.

### Variables
| Variable | Description | Default |
|----------|-------|---------|
| `schedule` | A cron string which controls how frequently new attacks are created | `0 0 * * * *` |

### Pausing Chaos

The Chaos Engine can be paused and prevented from generating more attacks. This is useful if a major demonstration is in order and the usual risk associated with chaos is temporarily unacceptable.

To pause and resume Chaos, use the following REST endpoint.

| Method | URI | Parameters |
| --- | --- | --- |
| POST | /admin/state | state=PAUSED |
| POST | /admin/state | state=STARTED |

### Holiday Calendar

The Chaos Engine will not run when the development team is not in the office. This includes Weekends, After Hours, and Holidays.

Determining when to consider a day a holiday, or when to consider a time after hours depends on where in the world the development team is.

#### Variables
| Variable | Effect | Default |Available Options
|----------|--------|--------|--------|
| `holidays` | Controls which country to load holidays from. Uses the ISO-3166-Alpha-3 Country Codes. | **CAN** |CAN, CZE, FRA |

## Notifications

Notifications can be sent out through various methods when Chaos Events occur.

### DataDog

#### Variables
| Variable | Effect | Default |
|----------|--------|--------|
| *`datadog_apikey`* | API Key for DataDog access. | \<None\> |

### Slack

Notifications can be sent to a slack channel when a Chaos Event occurs. Configure a Slack Webhook and provide the URI as an argument.

#### Variables
| Variable | Effect | Default |
|----------|--------|--------|
| *`slack_webhookuri`* | Web Hook URI for pushing to Slack. | \<None\> |


## Rest Controllers

Various REST endpoints are available for interacting with the Chaos Engine.
| URI | METHOD | Required Variables | Effect |
| :-: | :-: | :-- | --- |
| `/attack/{uuid}` | `GET` | - | Returns the attack against a container identified by `$uuid` |
| `/attack/` | `GET` | - | Returns a list of active attacks |
| `/attack/queue` | `GET` | - | Returns the contents of the pending new attack queue. |
| `/attack/start/{uuid}` | `POST` | - | Triggers an attack against the specific container identified by `$uuid` |
| `/attack/start` | `POST` | - | Triggers the chaos engine to start a new attack (same mechanism as when the `schedule` variable is hit, but without random chance) |
| `/health` | `GET` | - | System Health Check endpoint that can be used for IAAS/PAAS level health checks. Checks the API status of all platforms configured and validates they are working. |
| `/logging/{id}` | `GET` | - | Returns the logging level for the `$id` class path. |
| `/logging/{id}` | `POST` | *loggingLevel* | Sets the logging level for the `$id` class path to *loggingLevel* level (normal logging levels, i.e. DEBUG, INFO, WARN, ERROR...). |
| `/logging` | `GET` | - | Returns the logging level for the `com.gemalto` class path. |
| `/logging` | `POST` | *loggingLevel* | Sets the logging level for the `com.gemalto` class path to *loggingLevel* level (normal logging levels, i.e. DEBUG, INFO, WARN, ERROR...). |
| `/platform/health` | `GET` | - | Returns the health level of all platforms in Chaos. |
| `/platform` | `GET` | - | Returns a list of all platforms, including the roster of containers and the recent attack schedule. |




## Misc

### Logging
The Chaos Engine log level can be dynamically changed during runtime using a REST method located at `/logging`. A POST request with the new log level (of `ERROR`, `WARN`, `INFO`, `DEBUG`, or `TRACE`) will set the log level.

A path variable of the class to specify can also be used. (i.e., `/logging/org.springframework`, or `/logging/com.gemalto.notification`) to control specific elements. If no classpath is specified, `com.gemalto` is defaulted.

### Self preservation
The Chaos Engine will try to identify if it is running in the same environment that it is destroying. It will then try and prevent itself from matching itself as a chaos target. However, linked applications will need to be manually specified through environment variables. Specifics are listed with each Chaos target type.

### Notes
*`variables`* listed in italics are used to control if a feature is enabled. If those variables are specified, other dependant variables may also be logically required, but not programmatically required. This may cause run time errors.

