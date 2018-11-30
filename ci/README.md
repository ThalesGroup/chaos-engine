# Concourse Pipelines

These Concourse pipeline files are intended for easy management of the lifecycle of the Chaos Engine artifacts.

The pipelines are self-managed by the *pipeline-manager* pipeline. When configured and loaded into concourse, it will automatically build and maintain the remainder of pipelines from the Git repository.