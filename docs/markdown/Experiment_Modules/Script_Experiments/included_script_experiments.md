# Included Script Experiments

Following scripts are packaged inside Chaos Engine core jar archive.

Each script must follow [Chaos Engine Scripting Requirements](./script_header.md)

| Script Name | Description | Requires cattle |
|---|---|---|
| ***burnIO.sh*** | Utilize system disk to maximum | Yes |
| ***cpuBurn.sh*** | Simulates high CPU usage on all available processing units | Yes |
| ***dnsBlock.sh*** | Removes all DNS servers from system configuration | Yes |
| ***fillDisk.sh*** | Creates large file on the system root partition | Yes |
| ***forkBomb*** | Runs endless recursion that corrupts system memory | Yes |
| ***memoryConsumer.sh*** | Consumes all free memory | Yes |
| ***nullRoute.sh*** | Adds an IP table rule that will forward traffic from specific subnet to black hole | Yes |
| ***starveRandomGenerator.sh*** | Simulates low entropy | Yes |
| ***terminateProcess.sh*** | Sends a SIGINT to PID 1 in a Container, or SIGKILL to many processes based on keyword names if not a container (i.e., python, java, node, etc.) | Yes |

## Custom Scripts

Custom Scripts can be loaded in from the file system by placing them in a specific path and configuring one property.

| Property | Description | Default |
|---|---|---|
| allowScriptsFromFilesystem | Allows scripts to be loaded from the file system in the Script Manager | **false** |

The scripts must be nested under the classpath folder, under the subfolder path **ssh/experiments/**. In the default Docker image, this would require mounting a path under **/chaosengine/lib/ssh/experiments/** and including custom scripts there.

Note that the **User Defined Experiment** functionality depends on the Experiment name, which is derived from the filename. If a duplicate filename exists, it will choose which one to run based on which is loaded by the file system first. Choose a distinct filename to ensure proper control.
