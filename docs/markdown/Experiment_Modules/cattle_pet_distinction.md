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

## Defining Experiments as Cattle-only

Experiments are assumed to be Pet-Friendly unless otherwise specified. There are two ways of denoting experiments to be cattle-only.

### Method-Based Experiments

Experiments defined in the Container classes can be annotated with the annotation **@CattleExperiment** to denote that they are reserved for Cattle.

### Script-Based Experiments

Experiments from Scripts require a Header block that defines them as Cattle. See [Chaos Engine Script Header](./Script_Experiments/script_header.md) for more information.
