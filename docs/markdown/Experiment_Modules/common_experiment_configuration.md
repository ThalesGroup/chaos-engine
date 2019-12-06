#  Common Configuration
  
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
