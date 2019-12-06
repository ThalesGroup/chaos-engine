# Configuration Examples

## Enable EC2 Module
This example will enable EC2 module targeting all machines in `eu-west-1` region with `ChaosVictim` tag set to true.

!!! Note 
    `sshprivatekeys` is needed only in case you would like to perform shell based experiments from Chaos Engine build in suite.

```json tab="Vault"
{
"aws.ec2":"true",  
"aws.accessKeyId":"ABCDEFGHIJKLMNOPQRST",
"aws.secretAccessKey":"AbCDeFGHI+Jklmnop12345678789+123456789AB",
"aws.region":"eu-west-1",
"aws.ec2.filter.tag.ChaosVictim":"true",
"aws.ec2.averageMillisPerExperiment":"30000",
"aws.ec2.sshprivatekeys.your-key-name":"MIIEowIBABC ... EFQ="
}
```

``` shell tab="ENV Vars"
AWS_EC2="true"  
AWS_ACCESSKEYID="ABCDEFGHIJKLMNOPQRST"
AWS_SECRETACCESSKEY="AbCDeFGHI+Jklmnop12345678789+123456789AB"
AWS_REGION="eu-west-1"
AWS_EC2_FILTER_TAG_CHAOSVICTIM="true"
AWS_EC2_AVERAGEMILLISPEREXPERIMENT=30000
AWS_EC2_SSHPRIVATEKEYS_YOUR_KEY_NAME="MIIEowIBABC ... EFQ="
```

## Enable RDS Module

Following example enables RDS module. Experiments will be executed in `eu-west-3` region on top all DB instances with `ChaosVictim` tag set to true.

```json tab="Vault"
{
"aws.rds":"true",
"aws.rds.filter.ChaosVictim":"true",
"aws.rds.averageMillisPerExperiment":"300000",
"aws.accessKeyId":"ABCDEFGHIJKLMNOPQRST",
"aws.secretAccessKey":"AbCDeFGHI+Jklmnop12345678789+123456789AB",
"aws.region":"eu-west-3",
}
```

``` shell tab="ENV Vars"
AWS_RDS="true"
AWS_RDS_FILTER_CHAOSVICTIM="true"
AWS_RDS_AVERAGEMILLISPEREXPERIMENT="300000"
AWS_ACCESSKEYID="ABCDEFGHIJKLMNOPQRST"
AWS_SECRETACCESSKEY="AbCDeFGHI+Jklmnop12345678789+123456789AB"
AWS_REGION="eu-west-3"
```


## Enable PCF Module
When you use following example the Chaos Engine will run experiments on both possible PCF levels `application` and `container`.
If you need just one of them simply remove `cf.containerChaos` or `cf.applicationChaos` from your configuration.

```json tab="Vault"
{
  "cf.apihost": "api.my.pcf.com",
  "cf.organization": "chaos",
  "cf.space": "chaos",
  "cf.password": "pa$$Word-ABCDefgAbc",
  "cf.port": "443",
  "cf.username": "admin",
  "cf.averageMillisPerExperiment": "300000",
  "cf.containerChaos": "true",
  "cf.applicationChaos": "true"
}
```

``` shell tab="ENV Vars"
CF_APIHOST="api.my.pcf.com"
CF_ORGANIZATION="chaos"
CF_SPACE="chaos"
CF_PASSWORD="pa$$Word-ABCDefgAbc"
CF_PORT="443"
CF_USERNAME="admin"
CF_AVERAGEMILLISPEREXPERIMENT="300000"
CF_CONTAINERCHAOS="true"
CF_APPLICATIONCHAOS="true"
```

## Enable Kubernetes Module

Running Kubernetes experiments requires configuration on the server side please check [Kubernetes module manual](../Experiment_Modules/kubernetes_experiments.md)

```json tab="Vault"
{
  "kubernetes": "true",
  "kubernetes.url": "https://77.77.77.77",
  "kubernetes.token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyfQ.SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c",
  "kubernetes.averageMillisPerExperiment": "30000"
}
```

``` shell tab="ENV Vars"
KUBERNETES="TRUE"
KUBERNETES_URL="HTTPS://77.77.77.77"
KUBERNETES_TOKEN="EYJHBGCIOIJIUZI1NIISINR5CCI6IKPXVCJ9.EYJZDWIIOIIXMJM0NTY3ODKWIIWIBMFTZSI6IKPVAG4GRG9LIIWIAWF0IJOXNTE2MJM5MDIYFQ.SFLKXWRJSMEKKF2QT4FWPMEJF36POK6YJV_ADQSSW5C"
KUBERNETES_AVERAGEMILLISPEREXPERIMENT="30000"
```


## Enable Chaos Engine REST Security

This way you can enable Chaos Engine REST security. Following example will provision two users `admin` and `user`.

```json tab="Vault"
{
  "chaos.security.enabled": "true",
  "chaos.security.users[0].username": "admin",
  "chaos.security.users[0].password": "admin_P@ssw0rd",
  "chaos.security.users[0].roles": "ADMIN",
  "chaos.security.users[1].username": "user",
  "chaos.security.users[1].password": "user_P@ssw0rd",
  "chaos.security.users[1].roles": "USER"

}
```

``` shell tab="ENV Vars"
CHAOS_SECURITY_USERS_0_USERNAME=admin
CHAOS_SECURITY_USERS_0_PASSWORD=admin_P@ssw0rd
CHAOS_SECURITY_USERS_0_ROLES=ADMIN
CHAOS_SECURITY_USERS_1_USERNAME=user
CHAOS_SECURITY_USERS_1_PASSWORD=user_P@ssw0rd
CHAOS_SECURITY_USERS_1_ROLES=USER
```