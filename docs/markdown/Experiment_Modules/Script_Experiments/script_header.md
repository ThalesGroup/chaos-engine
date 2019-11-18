# Script Header

The header block of a Script loaded in the Chaos Engine Script Manager is parsed for several values that will be used to consider how the script is used.

## Example Header

``` shell tab="Script for Cattle"
#!/bin/sh
# Dependencies: dd
# Description: This will continuously read from /dev/random and drain entropy on the server.

while [[ true ]] ; do dd if=/dev/random of=/dev/null bs=1 count=1024 ; done
```

``` shell tab="Script for Pets"
#!/bin/sh
# Dependencies: whoami, echo, init
# Description: This will continuously read from /dev/random and drain entropy on the server.
# Health check: echo 'Hello, world'
# Self healing: init 6

whoami # This is admittedly a bad script for an experiment
```

The parsed fields require exact matches, including the space after the \#, as well as the colon immediately after the phrase.

| Property         | Description                                                                                                                                                                         |
|------------------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| Dependencies     | A comma separated list of binaries that need to be present on the server in order for the script to succeed. These will be evaluated using **which**, **type**, and **command -v** before the script is executed. Containers will persist a memory of which of these dependencies do not exist, and filter out future execution of scripts that have these missing dependencies.                       |
| Health check     | A command to be run to evaluate the health of the server during the experiment runtime. This command should produce a non-zero exit code if the experiment hasn't corrected itself. This is not used for cattle scripts, as they instead evaluate if the container was recycled.                                                                                         |
| Self healing     | A command to be run if an experiment has gone over duration. This command should have an effect which causes the Health Check command to return successfully.   This is not used for cattle scripts, as they instead recycled the container as a healing mechanism.                                                                                  |
| Finalize command | A command to be run after the experiment has finished, perhaps to correct something that we don't expect automation to fix, even though it appears health.  This is not used for cattle scripts, as they expected the container to be recycled, and thus, need no correction.                                                                    |
| Description      | No functional use, but leaves room to be exposed by APIs.                                                                                                                           |

# Cattle-Only Status

A script may be used by either Cattle and Pets, or just Cattle. This is determined by the presence of the **Health check** and **Self healing** commands.

If both commands are present, then the script is flagged as being pet-capable. It is expected that the Self healing and Finalize scripts will result in the system coming back to normal operation.

If neither of the commands are present, the script is flagged as cattle-only. It will only be run on systems that are backed by some form of controller that is capable of rebuilding the object (for example, an Autoscaling Group EC2 instance, or a Kubernetes Pod as part of a ReplicaSet).

If exactly one of the commands is present, an Exception is thrown during creation. This should also cause unit tests to fail, which should prevent the system from being built.
