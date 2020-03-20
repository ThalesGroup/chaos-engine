![Chaos Engine](/docs/markdown/images/chaos-engine-full.png)

![Sanity CI](https://github.com/ThalesGroup/chaos-engine/workflows/Sanity%20CI/badge.svg)
[![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=ThalesGroup_chaos-engine&metric=alert_status)](https://sonarcloud.io/dashboard?id=ThalesGroup_chaos-engine)
[![Lines of Code](https://sonarcloud.io/api/project_badges/measure?project=ThalesGroup_chaos-engine&metric=ncloc)](https://sonarcloud.io/dashboard?id=ThalesGroup_chaos-engine)
[![Coverage](https://sonarcloud.io/api/project_badges/measure?project=ThalesGroup_chaos-engine&metric=coverage)](https://sonarcloud.io/dashboard?id=ThalesGroup_chaos-engine)
[![Security Rating](https://sonarcloud.io/api/project_badges/measure?project=ThalesGroup_chaos-engine&metric=security_rating)](https://sonarcloud.io/dashboard?id=ThalesGroup_chaos-engine)
[![Reliability Rating](https://sonarcloud.io/api/project_badges/measure?project=ThalesGroup_chaos-engine&metric=reliability_rating)](https://sonarcloud.io/dashboard?id=ThalesGroup_chaos-engine)
[![Known Vulnerabilities](https://snyk.io/test/github/thalesgroup/chaos-engine/badge.svg)](https://snyk.io/test/github/thalesgroup/chaos-engine)

Chaos Engine is an application for creating random Chaos Events in cloud applications to test resiliency. It follows the [Principles of Chaos] to create random faults (*experiments*) that could reasonably occur in a real application deployment.

Chaos Engine makes intelligent decisions in how and when to create experiments. When properly configured, experiments can be restricted to occur only **during** normal business hours (i.e., no pager alerts).

## Supported Platforms

Chaos Engine currently supports Amazon Web Services, Google Cloud Platform, Pivotal Cloud Foundry, and Kubernetes. 

### :warning: Warning

Running chaos experiments in a non-resilient system can result in significant faults. We highly recommend you use a graduated approach to chaos implementation, and build confidence in your development and staging environments before attempting the same in your production environment.

## Capabilities 

| | Resource Faults | Communication Faults | Application Faults |
| --- | :---: | :---: | :---: |
| Kubernetes | :heavy_check_mark: | :heavy_multiplication_x: | :heavy_check_mark: |
| Cloud Foundry | :heavy_check_mark: | :heavy_check_mark: | :heavy_check_mark: |
| AWS EC2 | :heavy_check_mark: | :heavy_check_mark: | :heavy_check_mark: |
| AWS RDS | :heavy_multiplication_x: | :heavy_check_mark: | :heavy_multiplication_x: |
| GCP Compute | :heavy_check_mark: | :heavy_check_mark: | :heavy_check_mark: |

#### Legend
:heavy_check_mark: - Fully Supported | :white_check_mark: - Some Support
| :heavy_multiplication_x: - Not Supported | :construction: - Planned

For more information on how these experiments are done, please refer to the full [experiment modules documentation].

## Requirements

Chaos Engine does not specifically require the use of any other significant applications to already exist in your deployment. Each Cloud Platform is separately configured with access credentials.

The Chaos Engine instance runs in a Docker image, and will need network access to API endpoints for the appropriate platforms

See the [documentation] for more information.

## Getting Started

Please see our [documentation] for configuration instructions.

## Contributing

Chaos Engine is being actively developed, but we welcome public participation. Please note that we have internal processes that may slow down or require alterations to any pull requests. We may also have internal development on similar features. You can contact us for more information.


## Demo Video

[![Chaos Engine Introduction](https://img.youtube.com/vi/IX-gNYBLyDM/0.jpg)](https://www.youtube.com/watch?v=IX-gNYBLyDM) 

## Contact

You can [e-mail us], or find us on [Slack].

[Principles of Chaos]: http://principlesofchaos.org/
[documentation]: https://thalesgroup.github.io/chaos-engine/
[experiment modules documentation]: https://thalesgroup.github.io/chaos-engine/Experiment_Modules
[E-mail us]: mailto:dl_chaos_engine@gemalto.com
[Slack]: https://join.slack.com/t/thaleschaosengine/shared_invite/enQtODY1MDk1OTY4OTgyLTZjOGI5NzM1YTA2OWE5MjgzMWYxMzkwZjIwYTE3NjBlNDM4ZTkzNzc5YmMyMTU2Zjc5ODhlMTVkZDJhMmEzMzc
