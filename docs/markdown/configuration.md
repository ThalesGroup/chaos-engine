# Configuration

Chaos Engine reads configuration from multiple locations, and in multiple naming conventions.

## application.properties file
As a Spring Boot application, the application will look for a file named **application.properties** in the **./config** folder. This file should be formatted with one variable per line, in the format of `key=value`.

Line breaks can be made by adding a backslash (`\`) at the end of the line.

```text
aws.ec2.routableCidrBlocks=10.0.0.0/8,192.168.100.0/24
aws.ec2.filter.tag.chaosTestTag=chaosTesting
aws.ec2.filter.keyName=chaosTestKey
aws.ec2.privateSshKeys.chaosTestKey=-----BEGIN RSA PRIVATE KEY-----\
MIIEpgIBAAKCAQEAuENqLqKYT7vld6EvSK1myOH29dX2lb3sLEXcHybgKGr1kjjU\
...
cxsbp/4QHL+kwxzKqF6w3s6ZQ5sOh8vBoUf3RhdjM7NY7dQOHhUltfN6\
-----END RSA PRIVATE KEY-----
```

## Environment Variables
Environment variables can be read as configuration properties. Given their naming restrictions, Spring Framework remaps them intelligently such that an underscore can either represent a period or a new word with camelCaps.

| Environment Variable | Parsed as |
| --- | --- |
| AWS_EC2_ROUTABLE_CIDR_BLOCKS | aws.ec2.routableCidrBlocks |
| AWS_EC2_FILTER_TAG_CHAOS_TEST_TAG | aws.ec2.filter.tag.chaosTestTag |
| AWS_EC2_PRIVATE_SSH_KEYS_TEST_KEY | aws.ec2.privateSshKeys.testKey |

## Vault
Vault needs to be configured using one of the other methods. Once configured, properties from Vault are loaded in. Secrets need to be named in the same manner as in the [application.properties file configuration](#applicationproperties-file).