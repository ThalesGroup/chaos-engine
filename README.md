![Chaos Engine](/docs/markdown/images/chaos-engine-full.png)

Chaos Engine is an application for creating random Chaos Events in cloud applications to test resiliency. It follows the [Principles of Chaos] to create random faults (*experiments*) that could reasonably occur in a real application deployment.

Chaos Engine makes intelligent decisions in how and when to create experiments. When properly configured, experiments can be restricted to occur only **during** normal business hours (i.e., no pager alerts).

Chaos Engine currently supports Amazon Web Services, Pivotal Cloud Foundry, and Kubernetes. We have future plans to add support for Google Cloud Platform.

## Requirements

Chaos Engine does not specifically require the use of any other significant applications to already exist in your deployment. Each Cloud Platform is separately configured with access credentials.

The Chaos Engine instance runs in a Docker image, and will need network access to API endpoints for the appropriate platforms

See the [documentation] for more information.

## Contributing

Chaos Engine is being actively developed, but we welcome public participation. Please note that we have internal processes that may slow down or require alterations to any pull requests. We may also have internal development on similar features. You can contact us for more information.

## Contact

WIP


[Principles of Chaos]: http://principlesofchaos.org/
[documentation]: http://gemalto.github.com/chaos-engine/
