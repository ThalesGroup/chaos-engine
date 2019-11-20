# Chaos Engine Annotations

Custom annotations may be used to control the flow of other parts of the Chaos Engine application. This is a rundown of the annotations, and where they are used.

## **Identifier** Annotation

| | |
| --- | --- |
| Package | com.thales.chaos.container |
| Target | Fields |
| Purpose | Used in toString() and equals() implementations |

Container objects contain many fields that are needed for comparing equality. These fields are used as part of the object checksum calculation for equality, and  for converting the object to a string. The Identifier contains a **order** modifier. This integer value configures the sorting order for the fields to ensure consistency.

## **ChaosExperiment** Annotation

| | | 
| --- | --- |
| Package | com.thales.chaos.container |
| Target | Methods |
| Purpose | Flagging methods to be used as experiments |
 
Container objects contain many methods to be used as the source of an experiment. These methods should be tagged with the ChaosExperiment annotation. Java  Reflections are used to scan these objects to find the tagged methods. The methods need to take a single parameter, an Experiment object.

### Annotation Parameters
| Name | Type | Description | Default |
| --- | --- | --- | --- |
| experimentType | ExperimentType | The Type of experiment, based on what kind of core issue the application instance will experience, whether a state failure, resource failure, or network failure. | STATE |
| minimumDurationInSeconds | int | The minimum amount of time before an experiment can be evaluated for health. This time should consider how long it may take before a request for an action, such as stopping an instance, may actually take visible action. | 30 |
| maximumDurationInSeconds | int | The maximum amount of time before an experiment should proceed to self healing. This time should consider if an experiment may take a longer time to affect performance of a system. | 300 |
 | cattleOnly | boolean | Flags whether or not this experiment should only affect cattle-like containers (i.e., containers that can be disposed of and recreated as needed). Use this if you cannot naturally recover your experiment via self-healing. | false |
