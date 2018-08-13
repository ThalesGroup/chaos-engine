#!/usr/bin/env python3


def create(aws_access_key_id: str, aws_secret_access_key: str, aws_region: str, count: int) -> list:
    import boto3

    ec2 = boto3.resource('ec2', region_name=aws_region,
                         aws_access_key_id=aws_access_key_id,
                         aws_secret_access_key=aws_secret_access_key)

    instances = []
    index = 0
    for instance in ec2.create_instances(ImageId='ami-2a7d75c0',    MinCount=count,    MaxCount=count, InstanceType="t2.micro"):
        ec2.create_tags(
            Resources=[instance.id],
            Tags=[{
                'Key': 'Name',
                'Value': f'Test Instance {index}'
            },
                {
                'Key': 'Chaos Victim',
                'Value': 'true'
            }]
        )
        index += 1
        instances.append(instance.id)
        print(f"New instance {instance.id} created ({index}/{count})")
    print("done")
    return instances


def destroy(aws_access_key_id: str, aws_secret_access_key: str, aws_region: str, instances: list) -> None:
    import boto3
    print("Deleting existing EC2 instances")
    ec2 = boto3.client('ec2', region_name=aws_region,
                         aws_access_key_id=aws_access_key_id,
                         aws_secret_access_key=aws_secret_access_key)

    ec2.terminate_instances(InstanceIds = instances)
    print ("done")

def saveInstances(instances: list):
    import pickle
    with open(getPicklePath(), 'wb') as f:
        pickle.dump(instances, f)


def loadInstances() -> list:
    import pickle
    with open(getPicklePath(), 'rb') as f:
        return pickle.load(f)

def getPicklePath() -> str:
    import os
    scriptPath = os.path.dirname(__file__)
    return os.path.join(scriptPath, '.instances')


if __name__ == '__main__':
    import argparse
    import pickle
    import os
    parser = argparse.ArgumentParser()
    parser.add_argument("aws_access_key_id", type=str)
    parser.add_argument("aws_secret_access_key", type=str)
    parser.add_argument("--aws_region", "-r", type=str, default="eu-west-1")
    parser.add_argument("--number_of_instances", "-n", type=int, default=15)
    args = parser.parse_args()
    if os.path.isfile(getPicklePath()):
        destroy(args.aws_access_key_id, args.aws_secret_access_key,
                args.aws_region, loadInstances())
        os.remove(getPicklePath())
    else:
        saveInstances(create(args.aws_access_key_id,
                             args.aws_secret_access_key, args.aws_region, args.number_of_instances))
