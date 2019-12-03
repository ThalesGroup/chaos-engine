# Quick Start Guide

## Step 1: Install Docker
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

For detailed instructions on how to install docker compose [see.](https://docs.docker.com/compose/install/)

### Verify Docker Compose Installation
```bash
user@host:~$ docker-compose -v
docker-compose version 1.25.0, build 0a186604
```

If the command `docker-compose` fails after installation, check your path. You can also create a symbolic link to `/usr/bin` or any other directory in your `path`.

## Step 3: Download Latest Chaos Engine Sources
```bash
user@host:~$ git clone https://github.com/gemalto/chaos-engine
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
user@host:~/chaos-engine$ echo "holidays=DUM
VAULT_TOKEN=00000000-0000-0000-0000-000000000000
VAULT_SCHEME=http
VAULT_HOST=vault
VAULT_PORT=8200
VAULT_10=true
spring_profiles_active=PRODUCTION

automatedMode=false
CHAOS_SECURITY_ENABLED=false" > .env
```

### Configure Experiment Modules
Let's say you want to execute experiments targeting a Kubernetes cluster.
First check [experiment modules documentation](../Experiment_Modules/kubernetes_experiments.md) to see what config options are available then add all configuration properties into `vault-secrets.json` file located in `~/developer-tools/vault-loader`.
Example content of `vault-secrets.json `
```JSON
{
  "kubernetes": "",
  "kubernetes.url": "https://127.127.127.127",
  "kubernetes.token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyfQ.SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c",
  "kubernetes.averageMillisPerExperiment": "30000"
}
```


### Configure DataDog Integration
#### Enable DataDog
If you are going to ship data to DataDog run following command where `$YOUR_API_KEY` will be replaced by your real DataDog API key.

```bash
user@host:~/chaos-engine$ echo "DD_API_KEY=$YOUR_API_KEY" > .datadog_api_key 
```
#### Disable DataDog
If you don't need DataDog integration keep `.datadog_api_key` empty 
```bash
user@host:~/chaos-engine$ touch .datadog_api_key 
```

## Step 6: Run
Start the Engine by running `docker-compose up`
```bash
user@host:~/chaos-engine$ docker-compose up
```
When you see `Experiments total count: 0` your Chaos Engine instance is up and ready
```JSON
chaosengine_1   | {"@timestamp":"2019-11-28T18:07:36.491Z","@version":"1","message":"Experiments total count: 0","logger_name":"com.thales.chaos.experiment.ExperimentManager","thread_name":"chaos-10","level":"INFO","level_value":20000,"count":0,"env":"DEVELOPMENT","chaos-host":"b4bd5f0829d6@172.18.0.4"}

```
## Step 7: Enjoy
Manualy trigger experiment execution by running
```bash
user@host:~/chaos-engine$ curl -X POST "http://localhost:8080/experiment/start" -H  "accept: */*"
```