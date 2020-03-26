# PCF Experiments

The Chaos Engine Cloud Foundry Module is able to connect to a Cloud Foundry organization and interact Applications deployed in Cloud Foundry. It can interact with entire applications or individual application instances.

The Cloud Foundry module is split into two platforms that operate independently from one another. One Platform is designed to run experiments that affect entire applications as resources, while the other interacts with individual application instances.

## SDK

The official Pivotal Cloud Foundry Java Client is used to interact with Cloud Foundry.

Resource: <https://github.com/cloudfoundry/cf-java-client>

Version: 4.5.0.RELEASE

Maven Repositories:

-   <https://mvnrepository.com/artifact/org.cloudfoundry/cloudfoundry-operations>
-   <https://mvnrepository.com/artifact/org.cloudfoundry/cloudfoundry-client-reactor>

## Configuration

Several environment variables control how the Chaos Engine creates a connection to the Cloud Foundry manager.

| Key Name                | Description                                                                               | Default Value | Mandatory |
|-------------------------|-------------------------------------------------------------------------------------------|---------------| :---: |
| cf.apiHost              | The FQDN of the API Host to connect to.                                                   | None          | Yes |
| cf.port                 | The port used to communicate with the API Host                                            | 443           | Yes |
| cf.username             | The username to connect to the API Host with.                                             | None          | Yes |
| cf.password             | The password for the associated username.                                                 | None          | Yes |
| cf.organization         | The Organization within Cloud Foundry to connect to.                                      | None          | Yes |
| cf.space                | The Space within the Organization to connect to.                                          | default       | Yes |
| cf.applicationChaos     | The presence of this key enables the application level experiments.<br /><br />Please note that at least one of *cf.applicationChaos* or *cf.containerChaos* must be set  | N/A           | Yes |
| cf.containerChaos       | The presence of this key enables the container level experiments.                         | N/A           | Yes |

## Node Discovery

Node Discovery for Cloud Foundry results in two rosters. The first roster represents every application, while the second represents every instance of every application. This allows experiments to operate at multiple layers in Cloud Foundry.

### Mechanism

API: <https://apidocs.cloudfoundry.org/4.5.0/apps/list_all_apps.html>

The module calls the Applications List API from Cloud Foundry via the Java SDK. Multiple Container objects are created as the result of this call. One set of Containers represents each Application. Another set of Containers is created by iterating for each of the requested number of instances per application, and represents every individual instance.

### Self Awareness

The Chaos Engine is able to determine if it is running in Cloud Foundry. It does so by looking for a pair of environment variables that Cloud Foundry sets in the container where the application runs. These variables are **vcap.application.name** and **CF\_INSTANCE\_ID**. These values are compared to the nodes during discovery, and if there is a match, the node is dropped from the roster.

Resource: <https://docs.run.pivotal.io/devguide/deploy-apps/environment-variable.html>

## Experiments

## Container level experiments

### Application Instance Stopped

Stopping a Cloud Foundry instance validates a few things. Any chain of calls that pass through the instance should seamlessly be handled by other available instances. The instance should be able to resume operations and start taking requests once BOSH realizes a container is terminated, and software in the container should be capable of starting up without issue.

#### Mechanism

API: <https://apidocs.cloudfoundry.org/4.5.0/apps/terminate_the_running_app_instance_at_the_given_index.html>

The Application Instance Terminate is called through the Java SDK. This will stop the instance of an application. The BOSH manager is supposed to restart instances at this point.

#### Health Check Mechanism

API: <https://apidocs.cloudfoundry.org/4.5.0/apps/get_the_instance_information_for_a_started_app.html>

The Application Instance Summary information queried via the Java SDK. The specific InstanceID is filtered from the results, and the State of the container is checked.

#### Self Healing

API: <https://apidocs.cloudfoundry.org/4.5.0/apps/restage_an_app.html>

The Application  is restaged via the Restage Application API via the Java SDK.

### SSH Based Experiments

Cloud Foundry Containers support SSH Based experiments that can be used to simulate specific failure scenarios like high CPU or MEM consumption.

See [Script Experiments](./Script_Experiments/included_script_experiments.md) for more details.

#### Mechanism

API: <https://apidocs.cloudfoundry.org/4.5.0/info/get_info.html>

Resource: <https://docs.cloudfoundry.org/devguide/deploy-apps/ssh-apps.html>

A combination of information is used to create an SSH Connection. The Info API provides an SSH Endpoint Address and port. A separate API call provides a one time password to be used for the SSH connection.

A connection for the individual instance is created by using the application id and instance id as a username (Formatted as **cf:*ApplicationID*/*InstanceID***. This connection is passed to the SSH Manager to run SSH based experiments.

Entire process can be described by following manual steps:

Run *cf app MY-AWESOME-APP --guid* and record the GUID of your target app.

``` bash
$ cf app MY-AWESOME-APP --guid
abcdefab-1234-5678-abcd-1234abcd1234
```

Query the */v2/info* endpoint of the Cloud Controller in your deployment. Record the domain name and port of the *app\_ssh\_endpoint* field.

``` bash
$ cf curl /v2/info
{
...
"app_ssh_endpoint": "ssh.MY-DOMAIN.com:2222",
"app_ssh_host_key_fingerprint": "a6:14:c0:ea:42:07:b2:f7:53:2c:0b:60:e0:00:21:6c",
...
}
```

Run *cf ssh-code* to obtain a one-time authorization code that substitutes for an SSH password.

``` bash
$ cf ssh-code
E1x89n
```

Run your ssh or other command to connect to the app instance. For the username, use a string of the form *cf:APP-GUID/APP-INSTANCE-INDEX@SSH-ENDPOINT*, where *APP-GUID* and *SSH-ENDPOINT* come from the previous steps. For the port number, use the *SSH-PORT* recorded above. *APP-INSTANCE-INDEX* is the index of the instance you want to access.
With the above example, you ssh into the container hosting the first instance of your app by running the following command:

``` bash
$ ssh -p 2222 cf:abcdefab-1234-5678-abcd-1234abcd1234/0@ssh.MY-DOMAIN.com
```

#### Health Check Mechanism

API: <https://apidocs.cloudfoundry.org/4.5.0/apps/get_the_instance_information_for_a_started_app.html>

SSH base experiments use two phase health check mechanism:

1) The application instance summary information queried via the Java SDK. The specific InstanceID is filtered from the results, and the State of the container is checked.

2) (If defined) When step 1 confirms the container is healthy, ssh connection to the container is made and specific health check command is executed, if expected status code is return the healthy state is returned

3) Container is considered healthy when both previous steps return healthy state



## Application level experiments

### Application Scaling

Application Scaling tests verify that any autoscaling that is configured does not cause random issues as applications are scaled up or down. For instance, too many instances of one application may cause thread pool exhaustion on a dependent service.

#### Mechanism

API: <https://apidocs.cloudfoundry.org/4.5.0/apps/updating_an_app.html>

The Application Update API is called via the Cloud Foundry SDK Applications Scale interface. Containers are appropriately created or destroyed automatically.

#### Health Check Mechanism

API: <https://apidocs.cloudfoundry.org/4.5.0/apps/list_all_apps.html>

All Applications are listed and parsed for running state. This ensures that any dependency chains are still healthy.

#### Self Healing & Experiment Finalization

API: <https://apidocs.cloudfoundry.org/4.5.0/apps/updating_an_app.html>

The application is rescaled back to its original value through the same API by which the experiment was launched.

### Application Restart

Application Restart tests how resilient the platform is to a short downtime of an entire application. This can happen while an application is being updated with a hotfix, or if a bug in an application causes all instances to simultaneously crash.

Restarting your application stops your application and restarts it with the already compiled droplet. A droplet is a tarball that includes:

-   stack
-   buildpack
-   application source code

#### Mechanism

API: <https://apidocs.cloudfoundry.org/4.5.0/apps/creating_an_app.html>

[A Restart Application request is initiated through the Cloud Foundry SDK.](https://apidocs.cloudfoundry.org/4.5.0/apps/restage_an_app.html)

#### Health Check Mechanism

[API:](https://apidocs.cloudfoundry.org/4.5.0/apps/list_all_apps.html)<https://apidocs.cloudfoundry.org/4.5.0/apps/list_all_apps.html>

All Applications are listed and parsed for running state. This ensures that any dependency chains are still healthy.

### Application Restage

Application Restage tests how resilient the platform is to a short downtime of an entire application. This can happen while an application is being updated with a hotfix, or if a bug in an application causes all instances to simultaneously crash.

Restaging your application stops your application and restages it, by compiling a new droplet and starting it.

#### Mechanism

API: <https://apidocs.cloudfoundry.org/4.5.0/apps/restage_an_app.html>

A Restage Application request is initiated through the Cloud Foundry SDK.

#### Health Check Mechanism

API: <https://apidocs.cloudfoundry.org/4.5.0/apps/list_all_apps.html>

All Applications are listed and parsed for running state. This ensures that any dependency chains are still healthy.

### Unmap Application Route

Unmap application routes tests how the applications running in Cloud Foundry containers are resilient to short unavailability of resources accessible over network.

[Routes and Domains concept description](https://docs.cloudfoundry.org/devguide/deploy-apps/routes-domains.html)

#### Mechanism

API: <https://apidocs.cloudfoundry.org/4.5.0/routes_mapping/mapping_an_app_and_a_route.html>

A unmap route request is initiated through the Cloud Foundry SDK. When an application has multiple routes one of them is selected randomly and unmaped. When an application has no routes the experiment is skipped.

#### Health Check Mechanism

API: <https://apidocs.cloudfoundry.org/4.5.0/apps/list_all_apps.html>

All Applications are listed and parsed for running state. This ensures that any dependency chains are still healthy.

#### Self Healing & Experiment Finalization

API: <https://apidocs.cloudfoundry.org/4.5.0/routes_mapping/delete_a_particular_route_mapping.html>

In the end of the experiment the previously unmaped route is assigned back to the application

