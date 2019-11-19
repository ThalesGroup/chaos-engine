# Pipeline Design Philosophy

This page outlines the philosophy of the Chaos Engine build pipeline, and where we would like to reach with it.

## Desired State

The pipeline should exist in 8 distinct stages that are progressive.

1.  **Version** - The version number to be used is calculated, to be used by all future stages as needed.
2.  **Build** - Compilation and unit testing should be done.
3.  **Test** - External static testing should be done, such as Anti Virus, SonarQube, and BlackDuck scans are initiated
4.  **Packaging** - Docker image is built and tagged with the COMMIT SHA only.
5.  **Container Scanning** - Container package is scanned for vulnerabilities.
6.  **Deploy** (test) - The docker image is deployed to an environment that can be used for testing.
7.  **DAST** - Dynamic testing of the running Docker image can be done (i.e., ZAProxy)
8.  **Tear Down** - The deployed environment is torn down.
9.  **Container Registry Cleanup** - Unnecessary images are removed. Publishable images are retagged with the appropriate Version tag.

## Variants

The desired pipeline would have 4 distinct variants that are used for different purposes.

1.  Protected Tag based builds. These tags should be on verified stable versions with specific feature sets merged in. The full pipeline is automated, and the final step should retag images.
2.  *master* and *develop* branches. These branches should contain stable and near-stable versions of the code base. The full pipeline is automated, except for the Container Registry final step. This should be a Manual job that Allows Failure.
3.  Merge Request based builds. These builds should be temporary while awaiting approval. These should be automated up to Tear-Down, allowing a reviewer to access the environment if needed. Tear Down and Registry Cleanup should happen automatically
4.  All other branches and unprotected tags. These should be treated as developer editions. There are manual blocks at both the Package and Deploy stages. The repo cleanup stage is always run, and is delayed by a reasonable amount.