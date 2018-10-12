# Chaos Engine

The Chaos Engine is designed to intermittently destroy or degrade instances of applications running in cloud based infrastructure. These events are designed to occur while the appropriate resources are available to resolve the issue if the platform fails to do so on it's own.

This is designed to simulate real cloud based outages that could occur. The code running on the cloud infrastructure should be robust enough to automatically recover. If it is, then the chaos event is a valid proof of concept. If it fails to, then it exposes an issue that could have happened at a less opportunistic time.

See more here: https://confluence.gemalto.com/display/CLS/Chaos+Engine+Documentation

## Chaos Types
The following platforms act as entry points for the Chaos Engine. At least one needs to be configured.

### Cloud Foundry

https://confluence.gemalto.com/display/CLS/Chaos+Engine+Cloud+Foundry+Module

### Amazon AWS EC2

https://confluence.gemalto.com/display/CLS/Chaos+Engine+AWS+EC2+Module

### Amazon AWS RDS

https://confluence.gemalto.com/display/CLS/Chaos+Engine+AWS+RDS+Module


## Scheduling

The Chaos Engine is designed to run persistently, but only perform events on a certain schedule, and with exceptions. Here is how to control when chaos will occur.

### Holiday Calendar

The Chaos Engine will not run when the development team is not in the office. This includes Weekends, After Hours, and Holidays.

Determining when to consider a day a holiday, or when to consider a time after hours depends on where in the world the development team is.

https://confluence.gemalto.com/display/CLS/Chaos+Engine+Scheduler

### Pausing Chaos

The Chaos Engine can be paused and prevented from generating more experiment. This is useful if a major demonstration is in order and the usual risk associated with chaos is temporarily unacceptable.

To pause and resume Chaos, use the following `admin-controller` REST endpoint.

https://confluence.gemalto.com/display/CLS/Chaos+Engine+REST+API



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


https://confluence.gemalto.com/display/CLS/Chaos+Engine+REST+API



## Misc

### Logging
The Chaos Engine log level can be dynamically changed during runtime using a REST endpoint `logging-controler`. A POST request with the new log level (of `ERROR`, `WARN`, `INFO`, `DEBUG`, or `TRACE`) will set the log level.

A path variable of the class to specify can also be used. (i.e., `/logging/org.springframework`, or `/logging/com.gemalto.notification`) to control specific elements. If no classpath is specified, `com.gemalto` is defaulted.

https://confluence.gemalto.com/pages/viewpage.action?spaceKey=CLS&title=Chaos+Engine+REST+API

### Self preservation
The Chaos Engine will try to identify if it is running in the same environment that it is destroying. It will then try and prevent itself from matching itself as a chaos target. However, linked applications will need to be manually specified through environment variables. Specifics are listed with each Chaos target type.


### Building Chaos Engine Docker Image

#### Prerequisites

##### docker-compose 
docker-compose version 1.22.0 is required, [see install or upgrade instructions](https://docs.docker.com/compose/install/#install-compose)

##### docker engine
Docker version 18.06.0-ce is required, [see install or upgrade instructions](https://docs.docker.com/install/linux/docker-ce/ubuntu/#install-docker-ce-1)

#### Build steps
1. Prepare environment file called `.env` in the same location as your `docker-compose.yml`

    File example:
```
   AWS_ACCESS_KEY_ID=
   AWS_REGION=eu-west-1
   AWS_SECRET_ACCESS_KEY=
   holidays=CZE
   AWS_FILTER_KEYS=Chaos Victim
   AWS_FILTER_VALUES=true
```
2. Run `docker-compose build .`
3. Ignore following maven error
```
[ERROR] Failed to execute goal org.springframework.boot:spring-boot-maven-plugin:2.0.2.RELEASE:repackage (default) on project chaosengine: Execution default of goal org.springframework.boot:spring-boot-maven-plugin:2.0.2.RELEASE:repackage failed: Unable to find main class -> [Help 1]
```
4. Check new image has been created in your local docker registry `docker images`
5. Run the image `docker run --env-file=.env -it chaos-engine_chaosengine`
6. Verify all environment variables were set properly `docker exec -it ${ContainerID} /bin/sh`