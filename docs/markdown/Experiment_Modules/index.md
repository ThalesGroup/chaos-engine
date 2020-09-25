# Experiment Modules

Each Experiment Module is responsible for interacting with the API endpoint of its appropriate Cloud Platform or Orchestration tool. The Experiment Modules discover nodes that can be experimented upon, ensuring experiments keep a minimum blast radius, and performing the API calls necessary to create an experiment.

##  Common Configuration
  
  Some properties are common to all Experiment Modules. These are all configured with the same prefix as the main experiment component variables (i.e., **aws.ec2.averageMillisPerExperiment**).
  
  | Key Name                                 | Description                                             | Default            |
  |------------------------------------------|---------------------------------------------------------|--------------------|
  | ${prefix}.averageMillisPerExperiment     | The average number of Milliseconds between experiments. | 14400000 (4 Hours) |
  
##### Available prefixes
  
  | Prefix     | Description               |
  |------------|---------------------------|
  | aws.ec2    | AWS EC2 experiments       |
  | aws.rds    | AWS RDS experiments       |
  | cf         | Cloud Foundry experiments |
  | kubernetes | Kubernetes experiments    |
  | gcp | Google cloud platform experiments |
