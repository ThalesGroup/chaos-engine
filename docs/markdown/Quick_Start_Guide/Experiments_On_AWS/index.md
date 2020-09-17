# AWS Experiments

In this quick start guide we will be demonstrating an experimets with AWS cloud platform.

## Chaos engine host configuration on AWS EC2 instance.
Choas engine run based on container. Please use the Linux OS to configure chaos engine host.  

### Pre-requisites. 
Please provision EC2 linux instace. In this scenario we will be provising an Ubuntu 18.04 EC2 instance on which you need to install Docker and Docker compose. Which will be used as Chaos engine host machine. 

* We will have to first create AWS EC2 instance. 
* If you have an AWS Account login to AWS management consol. If not, set up a free tier instance in Amazon’s * EC2. From the link, click Try Amazon EC2 for Free and enter the requested details to create your account.
Once you have signed up, log in to your account.
* Spin up/provision couple of Linux (Ubuntu) EC2 instance for chaos engine. 
* Follow the EC2 instance creation [instructions here](https://docs.aws.amazon.com/efs/latest/ug/gs-step-one-create-ec2-resources.html). 
* In this scenario we will have total 3 AWS EC2 instance one is for host purpose and remaining 2 are target instance. 
* Open the Amazon EC2 dashboard. Find your new running instance in the list and click to highlight it. Click Connect for information on how to connect to this instance using SSH.

Following is the snippet from AWS console on EC2 instance. 

instance_list.JPG

```bash
$ ssh -i "YourKeyName.pem" ubuntu@publicDNSname
```

After successful login please start installation of chaos engine host. 

## Step 1: Docker Installation
Follow the Docker instalation [instructions here](https://docs.docker.com/install/)

### Verify Docker Installation
To verify Docker installation run `docker ps`.

Expected output is:
```bash
user@host:~$ docker ps
CONTAINER ID        IMAGE               COMMAND             CREATED             STATUS              PORTS               NAMES
```

If the output looks similar to following example open new terminal session or simply logout and login again.

```bash
user@host:~$ docker ps
Got permission denied while trying to connect to the Docker daemon socket at unix:///var/run/docker.sock: Get http://%2Fvar%2Frun%2Fdocker.sock/v1.40/containers/json: dial unix /var/run/docker.sock: connect: permission denied
```

## Step 2: Install Docker Compose

For detailed instructions on how to install docker compose [see official Docker documentation.](https://docs.docker.com/compose/install/)

### Verify Docker Compose Installation
```bash
user@host:~$ docker-compose -v
docker-compose version 1.25.0, build 0a186604
```

If the command `docker-compose` fails after installation, check your path. You can also create a symbolic link to `/usr/bin` or any other directory in your `path`.

## Step 3: Download Latest Chaos Engine Sources
```bash
user@host:~$ git clone https://github.com/thalesgroup/chaos-engine
Cloning into 'chaos-engine'...
...
Checking connectivity... done.
```
## Step 4: Pull Chaos Engine Image

Pull latest Chaos Engine image from DockerHub.

```bash
user@host:~$ cd chaos-engine/
user@host:~/chaos-engine$ docker pull thalesgroup/chaos-engine:latest
```

## Step 5: Configure

### Basic Framework Setup

```bash
user@host:~/chaos-engine$ echo "holidays=NONSTOP
VAULT_TOKEN=00000000-0000-0000-0000-000000000000
VAULT_SCHEME=http
VAULT_HOST=vault
VAULT_PORT=8200
VAULT_10=true
SPRING_PROFILES_ACTIVE=DEVELOPMENT

automatedMode=false
CHAOS_SECURITY_ENABLED=false" > .env
```

### Configure Experiment Modules
Let's say you want to execute experiments targeting a Kubernetes cluster.
First check [experiment modules documentation](../../Experiment_Modules/kubernetes_experiments.md) to see what config options are available then add all configuration properties into `vault-secrets.json` file located in `~/developer-tools/vault-loader`.
Example content of `vault-secrets.json `
```JSON
{
  "kubernetes": "",
  "kubernetes.url": "https://127.127.127.127",
  "kubernetes.token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyfQ.SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c",
  "kubernetes.averageMillisPerExperiment": "30000"
}
```
Example content of EC2 instance for `vault-secrets.json`
```JSON
{
"aws.ec2":"true",  
"aws.accessKeyId":"<access_key_id>",  
"aws.secretAccessKey":"<Secrate_access_String>", 
"aws.region":"us-east-1",  
"aws.ec2.filter.tag.ChaosVictim":"true", 
"aws.ec2.averageMillisPerExperiment":"30000",
"aws.ec2.sshprivatekeys.<your_Key_Name>":"<private_key>”  
}
```
## Importance Note:- 
•	Please make sure you grant access to your IAM user to have FullEC2 access. 
•	The “ChaosVictim” = “true” tag must be existing in the target EC2 instances.
•	Please allow all the required ports 80, 8080, 8200 and 22. 


### Configure DataDog Integration
#### Enable DataDog
First of all, you have to sign up with data-dog to enable the integration with chaos-engine. 

Kindly go to the below URL to create your account in data-dog. 
[Data Dog sign up](https://www.datadoghq.com/)
You will get 14 days free trial account. Please sign up for free account for now to test it. 

Once you have account setup you, login to datadog portal -> API’s -> hover the mouse on key and your API Key will be displayed. 

<img src=datadog_apikey.JPG>

If you are going to ship data to DataDog run following command where `$YOUR_API_KEY` will be replaced by your real DataDog API key.

```bash
user@host:~/chaos-engine$ echo "DD_API_KEY=$YOUR_API_KEY" > .datadog_api_key 
```
#### Disable DataDog
If you don't need DataDog integration keep `.datadog_api_key` empty 
```bash
user@host:~/chaos-engine$ touch .datadog_api_key 
```

## Step 6: Start chaos engine host
Start the Engine by running `docker-compose up`
```bash
user@host:~/chaos-engine$ docker-compose up
```
When you see `Experiments total count: 0` your Chaos Engine instance is up and ready
```JSON
chaosengine_1   | {"@timestamp":"2019-11-28T18:07:36.491Z","@version":"1","message":"Experiments total count: 0","logger_name":"com.thales.chaos.experiment.ExperimentManager","thread_name":"chaos-10","level":"INFO","level_value":20000,"count":0,"env":"DEVELOPMENT","chaos-host":"b4bd5f0829d6@172.18.0.4"}

```
Once you are able to see the above logs in chaos host that means you are ready for chaos engine experiments. You can looks for the logs related to Chaos engine host in data dog as well. Following is snippet from the data dog. 

# Chaos Engine Experiments
Each Experiment Module is responsible for interacting with the API endpoint of its appropriate Cloud Platform or Orchestration tool. The Experiment Modules discover nodes that can be experimented upon, ensuring experiments keep a minimum blast radius, and performing the API calls necessary to create an experiment. Kindly refer the below URL for the same. 

[Experiment Modules](https://thalesgroup.github.io/chaos-engine/Experiment_Modules/)

There are two modes of experiments in chaos engine. 
1.	Automated mode
In automated mode experiment selection as well as platform is done randomly.
2.	User defined mode.
In user define experiment you can run shell and API experiments. In user define experiments you can choose the platform as well as the experiment type. In user define experiments you need to provide input in order to run the experiments.

<b> <u> Note: </u> </b> - Shell experiments require Cattle resources, that means your target resource must be backed by some kind of auto scaling mechanism. E.g. in EC2 resource case the instance must be backed by ASG. In K8S resource case the pod must have a deployment with more than one replicas etc.

[User define experiments](https://thalesgroup.github.io/chaos-engine/Core_Modules/experiment_manager/#user-defined-experiments)
The parameter for a User Defined Experiment is a single object with two variables. The platformType variable should be the name of the Platform you wish to experiment on. The experimentCriteria variable requires an object of <b> <u> containerIdentifier, experimentMethods[], and optional specificContainerTargets[]</u> ,</b>, to identify the aggregate container group, the type of experiments to run, and any specific targets which may not be identical (i.e., a MongoDB Primary node).

The Experiment Structure object can be sent as the data stream of a POST request to /experiment/build to create the experiments. The server expects a Content-Type of application/json. See the REST API for more information.
Following is the sample JSON for user defined experiments. You may find more information about this at 

## Example of user define API experiments
``` JSON
{
    "platformType": "KubernetesPlatform",
    "experimentCriteria": [
        {
            "containerIdentifier": "nginx-deployment-574b87c764",
            "experimentMethods": [
                "deletePod"
            ],
            "specificContainerTargets": [ ]
        }
    ]
}
```

## Example of user defined Shell experiments
``` JSON
{
    "platformType": "KubernetesPlatform",
    "experimentCriteria": [
        {
            "containerIdentifier": "nginx-deployment-574b87c764",
            "experimentMethods": [
                "forkBomb.sh"
            ],
            "specificContainerTargets": [ ]
        }
    ]
}
```

# Experiment Modules
Following are the list of experiments are available in chaos engine. Kindly refer the below url for list of available experiments.

[Experiments Moudle](https://thalesgroup.github.io/chaos-engine/Experiment_Modules/)

<b> AWS EC2 instance </b>
•	Stop 
•	Restart
•	Deletion of Security Group. 
•	Instance termination only in Auto Scaling Group. 
•	Shell experiments.

<b> Kubernetes </b>
•	Delete at POD layer.
•	Shell experiments at container layer. 

<b> AWS RDS </b>
•	Take a snapshot
•	Restart
•	Deletion of security group. 
•	Random restart of cluster nodes.
•	Failover.

<b> Pivotal Cloud foundry </b>
•	Rescale 
•	Restart 
•	Restage
•	Shell Experiments

<b> Shell based experiments. </b>
•	BurnIO 
•	CPUBurn
•	DNS Block
•	Fill disk
•	Forkbomb
•	Ram Consumer
•	Null route
•	Generator starvation
•	Process killer


# Run Experiments 
In order to run the experiments, make sure the at docker-compose is up and running on chaos engine host. 
After that you can login to Swagger UI to run the experiments. Following is the URL for swagger UI you need to replace IP address of you host. 
[Swagger UI](http://34.229.67.150:8080/swagger-ui.html#/)

<img src=swaggerui.JPG> 
 
To check the available targets/platform for the experiments you can execute following from swagger UI. 
Look for the platform section and click on Get /Platform  try it out. 
 
<img src=getplatform.JPG>

Click on execute. 

<img src=execute.JPG>

Refer the response body section. In the below screen shot you can see instance name details, which we have created as a targets to perform the experiments. There are total 2 instance or targets are available for experiments. 

<img src=response.JPG>
 
<img src=response1.JPG> 

Next go the Experiments section and run the experiments. <b> POST /experiments/start. </b> 

<img src=experiment.JPG>
 
Now I am going to start the experiment by clicking on /experiment/start (Start Random Experiment)
Click on /experiment/start -> try it out -> Execute. 

<img src=experiment1.JPG>

<img src=responsecode.JPG>
 
The chaos engine has chosen <b> “removeSecurityGroups” </b> experiment randomly.  It will remove the security group for an EC2 instance. Kindly refer the below screen shot from the AWS console. 

<img src=removsg.JPG>

Logs from chaos engine host. 
```bash
chaosengine_1   | {"@timestamp":"2020-09-09T06:36:24.124Z","@version":"1","message":"Evaluated experiment: Experiment of removeSecurityGroups against chaosengtarget2 (Sabilchaos) [i-06963df97e880ef98]","logger_name":"com.thales.chaos.experiment.ExperimentManager","thread_name":"ForkJoinPool-4-worker-3","level":"INFO","level_value":20000,"experimentMethod":"removeSecurityGroups","host":"i-06963df97e880ef98","experimentid":"880f3119-a86e-4420-adbf-dee8bdfdd21f","platform":"AwsEC2Platform","experiment":{"id":"880f3119-a86e-4420-adbf-dee8bdfdd21f","experimentState":"STARTED","container":{"shellCapabilities":{},"availabilityZone":"us-east-1a","instanceId":"i-06963df97e880ef98","keyName":"Sabilchaos","name":"chaosengtarget2","publicAddress":"54.227.31.237","imageId":"ami-0761dd91277e34178","groupIdentifier":"No Grouping Identifier Found","simpleName":"chaosengtarget2 (Sabilchaos) [i-06963df97e880ef98]","routableAddress":"54.227.31.237","aggregationIdentifier":"chaosengtarget2","cattle":false,"sshcapable":false,"containerType":"AwsEC2Container","experimentStartTime":1599633339.884297000,"identity":3165379631,"knownMissingCapabilities":[]},"experimentType":"NETWORK","selfHealingMethod":{},"startTime":1599633339.884297000,"lastSelfHealingTime":null,"selfHealingCounter":0,"experimentMethodName":"removeSecurityGroups","experimentLayerName":"AwsEC2Container","wasSelfHealingRequired":null},"env":"PROD","chaos-host":"bb8c3770c057@aws:i-086601e7b0a84aa54:257969174391:us-east-1"}
```
Logs from Data dog. 

<img src=datadaoglog.JPG>

<img src=datadaoglog1.JPG>
 
After completion of experiment you can see chaos engine has reverted Security Group. Below is the snippet for the same. 
 
<img src=revertsg.JPG>

Note: - you can configure/enable monitoring tool to get the real time analysis of a targets. 



## Step 7: Enjoy
Manualy trigger experiment execution by running
```bash
user@host:~/chaos-engine$ curl -X POST "http://localhost:8080/experiment/start" -H  "accept: */*"
```


# Summary
Congratulations, you just ran your first chaos engine experiment! Feel free to run the experiment many times, varying parameters and seeing how that impacts your instance. Next steps could be to add a full monitoring solution to the instance for better data. (we recommend doing that with a non-production, non-vital server at this stage, should you go this route).

When you are done testing using the Amazon instance created for this article, don’t forget to terminate the Amazon instance. 

Best wishes and have fun unleashing the chaos engine! :)


# References 
* [Chaos Engie](https://thalesgroup.github.io/chaos-engine/)
* [Experiment Module](https://thalesgroup.github.io/chaos-engine/Experiment_Modules/)
* [Data Dog](https://www.datadoghq.com/)
* [Ec2 Resource creation](https://docs.aws.amazon.com/efs/latest/ug/gs-step-one-create-ec2-resources.html)
* [User defined experiments](https://thalesgroup.github.io/chaos-engine/Core_Modules/experiment_manager/#user-defined-experiments)
* [Kubernetes Experiments](https://thalesgroup.github.io/chaos-engine/Experiment_Modules/kubernetes_experiments/#required-kubernetes-cluster-configuration)
* [Chaos Engines slack support forum](https://app.slack.com/client/TFJUJDNN9/CFJ10E27J)