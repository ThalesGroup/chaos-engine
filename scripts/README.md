# Scripts

## ec2_instance_provisioning.py

### Description
Script for automated provisioning of test EC2 instances.
A subsequent run will terminate the provisioned instances.

### Requirements
1. Python 3.6
2. boto3

### Mandatory Parameters
1. aws_region
2. aws access key id
3. aws secret access key

#### Execution example
```
    python3 ec2_instance_provisioning.py -r eu-west-1 AKI********* OiBI********************
```

