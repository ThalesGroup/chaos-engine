# Cattle/Pet Distinction

## Definition

There are two inherit types of objects in cloud platforms.
Pets are objects that were created by hand and are meticulously managed.
The process for replacing a pet is intensive.
Conversely, Cattle are objects that are created by some form of automation. 
It is expected that if a Cattle container is unhealthy, rather than repair, it is replaced. 

Some experiments may be destructive in nature. These experiments should only ever be applied to Cattle, and not to Pets. 
Conversely, some cloud providers may prevent actions on objects they consider "managed", meaning some experiments can only be done to Pets.
Both Containers and Experiments need to be appropriately tagged.

## Defining Containers as Cattle

The base Container class has a method called **isCattle()**. The default value for this is false. This can be Overriden in any Container class to be evaluated according to the nature of the specific container.

For example, an EC2 container may evaluate if it is part of an Autoscaling Group, while a Kubernetes Pod Container may evaluate if it is backed by a Replica Set.

## Defining Experiment Scopes


Experiments are assumed to be compatible with both ways of running experiments unless otherwise specified. There are two ways of denoting experiment scope.

### Method-Based Experiments

Method Based experiments use the **@ChaosExperiment** annotation for discovery. The **experimentScope** field of this annotation controls the targeting criteria.

### Script-Based Experiments

Experiments from Scripts require a Header block that defines them as Cattle. See [Chaos Engine Script Header](./Script_Experiments/script_header.md) for more information.
