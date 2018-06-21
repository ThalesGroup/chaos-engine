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
| PLATFORM | No  | Does not support interfering with the platform between containers (i.e., routes) |

#### Variables
| Variable | Description | Default |
|---|---|---|
| *`cf_apihost`* | Cloud Foundry API URI | \<None\> |
| `cf_port` | Cloud Foundry API Port | `443` |
| `cf_username` | Cloud Foundry Username | \<None\> |
| `cf_password` | Cloud Foundry Password | \<None\> |
| `cf_organization` | Cloud Foundry Organization | \<None\> |
| `cf_space` | Cloud Foundry Space | `default` |

### All Chaos Types

#### Variables
| Variable | Description | Default |
|----------|-------|---------|
| `probability` | A float value that controls how likely a container is to experience a chaos event | `0.2` |



## Scheduling

The Chaos Engine is designed to run persistently, but only perform events on a certain schedule, and with exceptions. Here is how to control when chaos will occur.

#### Variables
| Variable | Description | Default |
|----------|-------|---------|
| `schedule` | A cron string which controls how frequently Chaos is run | `0 0 * * * *` |

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
| `holidays` | Controls which country to load holidays from. Uses the ISO-3166-Alpha-3 Country Codes. | **CAN** |CAN, CZE

## Notifications

Notifications can be sent out through various methods when Chaos Events occur.

### DataDog

#### Variables
| Variable | Effect | Default |
|----------|--------|--------|
| *`datadog_apikey`* | API Key for DataDog access. | \<None\> |

### Slack
#### Variables
| Variable | Effect | Default |
|----------|--------|--------|
| *`slack_webhookuri`* | Web Hook URI for pushing to Slack. | \<None\> |



## Misc

*`variables`* listed in italics are used to control if a feature is enabled. If those variables are specified, other dependant variables may also be logically required, but not programmatically required. This may cause run time errors.

