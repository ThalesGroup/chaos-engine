![Chaos Engine](/docs/markdown/images/chaos-engine-full.png)

[![Coverage](https://sonarcloud.io/api/project_badges/measure?project=gemalto_chaos-engine&metric=coverage)](https://sonarcloud.io/dashboard?id=gemalto_chaos-engine)
[![Known Vulnerabilities](https://snyk.io/test/github/gemalto/chaos-engine/badge.svg)](https://snyk.io/test/github/gemalto/chaos-engine)

Chaos Engine is an application for creating random Chaos Events in cloud applications to test resiliency. It follows the [Principles of Chaos] to create random faults (*experiments*) that could reasonably occur in a real application deployment.

Chaos Engine makes intelligent decisions in how and when to create experiments. When properly configured, experiments can be restricted to occur only **during** normal business hours (i.e., no pager alerts).

## Supported Platforms

Chaos Engine currently supports Amazon Web Services, Pivotal Cloud Foundry, and Kubernetes. We have future plans to add support for Google Cloud Platform.

### :warning: Warning

Running chaos experiments in a non-resilient system can result in significant faults. We highly recommend you use a graduated approach to chaos implementation, and build confidence in your development and staging environments before attempting the same in your production environment.

## Capabilities 

| | Resource Faults | Communication Faults | Application Faults |
| --- | :---: | :---: | :---: |
| Kubernetes | :heavy_check_mark: | :heavy_multiplication_x: | :heavy_check_mark: |
| Cloud Foundry | :heavy_check_mark: | :heavy_check_mark: | :heavy_check_mark: |
| AWS EC2 | :heavy_check_mark: | :heavy_check_mark: | :heavy_check_mark: |
| AWS RDS | :heavy_multiplication_x: | :heavy_check_mark: | :heavy_multiplication_x: |
| GCP Compute | :construction: | :construction: | :construction: |

#### Legend
:heavy_check_mark: - Fully Supported | :white_check_mark: - Some Support
| :heavy_multiplication_x: - Not Supported | :construction: - Planned

For more information on how these experiments are done, please refer to the full [documentation].

## Requirements

Chaos Engine does not specifically require the use of any other significant applications to already exist in your deployment. Each Cloud Platform is separately configured with access credentials.

The Chaos Engine instance runs in a Docker image, and will need network access to API endpoints for the appropriate platforms

See the [documentation] for more information.

## Getting Started

Please see our [documentation] for configuration instructions.

## Contributing

Chaos Engine is being actively developed, but we welcome public participation. Please note that we have internal processes that may slow down or require alterations to any pull requests. We may also have internal development on similar features. You can contact us for more information.

## Contact

You can [e-mail us], or find us on [Slack].

[Principles of Chaos]: http://principlesofchaos.org/
[documentation]: https://gemalto.github.io/chaos-engine/
[E-mail us]: mailto:dl_chaos_engine@gemalto.com
[Slack]: https://join.slack.com/t/thaleschaosengine/shared_invite/enQtODY1MDk1OTY4OTgyLTZjOGI5NzM1YTA2OWE5MjgzMWYxMzkwZjIwYTE3NjBlNDM4ZTkzNzc5YmMyMTU2Zjc5ODhlMTVkZDJhMmEzMzc
