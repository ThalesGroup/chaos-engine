# 
![Chaos Engine Full Logo](./images/chaos-engine-full.png)

Chaos Engine is an application for creating random Chaos Events in cloud applications to test resiliency. It follows the [Principles of Chaos](http://principlesofchaos.org/) to create random faults (*experiments*) that could reasonably occur in a real application deployment.

Chaos Engine makes intelligent decisions in how and when to create experiments. When properly configured, experiments can be restricted to occur only **during** normal business hours (i.e., no pager alerts).

Chaos Engine currently supports Amazon Web Services, Pivotal Cloud Foundry, and Kubernetes. We have future plans to add support for Google Cloud Platform.

!!! warning
    Running chaos experiments in a non-resilient system can result in significant faults. We highly recommend you use a graduated approach to chaos implementation, and build confidence in your development and staging environments before attempting the same in your production environment.

## Supported Experiments

### Amazon Web Services

#### EC2 Instances

| Experiment |Target| Description |
| --- | --- | --- |
| Stop Instance | All EC2 Instances | Stops selected instance |
| Restart Instance | All EC2 Instances | Restarts selected instance |
| Remove Security Groups | All EC2 Instances| Removes all assigned security groups|
| Instance Termination | EC2 Instance in ASG only | Terminates an instance when it is running in ASG |
| Deploy Shell Experiment | EC2 Instance in ASG only | Deploys an experiment from shell experiment suite described below |

#### RDS Instances

| Experiment |Target| Description |
| --- | --- | --- |
| Restart Instance | All RDS Instances | Restarts selected instance|
| Remove Security Groups | All RDS Instances| Removes all assigned security groups|
| Take Snapshot | All RDS Instances| Takes a snapshot of the DB|
| Restart Subset of Nodes | RDS Cluster only| Randomly selects set of nodes and restarts them|
| Initiate failover | RDS Cluster only | Initialize failover between nodes |

### Kubernetes

| Experiment |Target| Description |
| --- | --- | --- |
| Delete POD | POD | Deletes randomly selected pod|
| Deploy Shell Experiment | Container | Deploys an experiment from shell experiment suite described below |

### Pivotal Cloud Foundry 

| Experiment |Target| Description |
| --- | --- | --- |
| Rescale Application | Application | Rescales an application to random number of instances |
| Restage Application | Application | Redeploys an application |
| Restart Application | Application | Restarts all application containers |
| Restart Instance | Container | Restarts selected container |
| Deploy Shell Experiment | Container | Deploys an experiment from shell experiment suite described below |

### Shell Experiments Suite

| Experiment |Target| Description |
| --- | --- | --- |
| BurnIO | EC2, PCF or Kubernetes resource supporting remote command execution | Utilize system disk to maximum |
| CPU Burn | EC2, PCF or Kubernetes resource supporting RCE | Simulates high CPU usage on all available processing units |
| DNS Block | EC2, PCF or Kubernetes resource supporting RCE | Removes all DNS servers from system configuration |
| Fill Disk | EC2, PCF or Kubernetes resource supporting RCE | Creates large file on the system root partition |
| Fork Bomb | EC2, PCF or Kubernetes resource supporting RCE | Runs endless recursion that corrupts system memory |
| Memory Consumer | EC2, PCF or Kubernetes resource supporting RCE | Consumes all free memory |
| Null Route | EC2, PCF or Kubernetes resource supporting RCE | Adds an IP table rule that will forward traffic from specific subnet to black hole |
| Random Generator Starvation | EC2, PCF or Kubernetes resource supporting RCE | Simulates entropy starvation |
| Process Termination | EC2, PCF or Kubernetes resource supporting RCE | Terminates random process |
