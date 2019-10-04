# Scheduler

The Chaos Engine uses an advanced scheduler to ensure sufficiently random time between failures, while simultaneously ensuring that experiments only occur while there are resources available to fix any issues that are not automatically resolved.

## Configuration
| Key Names | Description | Default | Notes |
| :---: | --- | :---: | --- |
| holidays | Configures which country's Holidays and Working Hours to follow. Available: CAN, CZE, FRA, USA | `CAN` | ISO-3166-Alpha-3 country codes are used |
| automatedMode | Configures if experiments should be run automatically (*true*), or only by API (*false*) | `true` | | 

## Behaviour

The Chaos Engine should only run experiments when the appropriate resources are available to remedy any Chaos Incidents that may be found in the process of running Chaos experiments, for which the Engine is not automatically able to resolve. The country selection implements default working hours for which the Engine will schedule experiments on Monday to Friday.

The chosen country will also calculate observed holidays. It is assumed that most or all employees will be unavailable to resolve Chaos Incidents on these days in a timely manner, so the Chaos Engine will not trigger automatic experiments on these days. This does not prevent starting explicit experiments using the Chaos Engine API.

In addition to the specific observed holidays, the Chaos Engine will also isolate any single abandoned days. If, for example, a holiday occurs on a Tuesday, it is assumed that the accompanying Monday will have a higher than normal amount of employees on vacation to take advantage of the long weekend. The Monday will also be flagged as an observed holiday. The same is true for Friday given an observed holiday on Thursday.

Finally, December 24th through January 1st are considered holidays due to the large amount of vacation taken during that periods.

## Random Experiment Timing

Each Experiment Module has its own independent configuration for **AverageMillisPerExperiment**. This value controls how the Scheduler calculates when the next experiment will occur. 

An algorithm uses a random number with a Gaussian distribution to seed the percentile of time between experiments. This percentile is then fed through a function that, given infinite iterations, will tend to average with the configured value above. Because the distribution is only bounded on the lower end, it is possible for long times between experiments (up to ~16**x** the configured average). As a result, the **median** time between experiments will be lower than the **average** time between experiments.

The time between experiments only counts time within office hours. When the scheduler reaches the end of the work day, it stops counting until the beginning of the next day.

## Disabling Automated Mode

By default, Chaos Engine generates automated tests. To disable this behaviour, you can use the `automatedMode` configuration flag. In addition, you can use a [REST API] to toggle this flag at runtime.


[REST API]: /rest