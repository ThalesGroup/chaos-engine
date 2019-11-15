# Chaos Engine Configuration

Chaos Engine can be configured from multiple sources. In addition, the naming conventions take advantage of Spring Relaxed Bindings, meaning a property can be identified with multiple naming conventions.

Each modules own documentation will indicate what specific configuration it requires.

## Configuration Sources

### **application.properties** File

Spring Boot looks for a file named **application.properties** under the **./config** directory to load properties from. This file should be formatted in **key=value** format. Use backslashes to preserve line breaks.

```shell tab="application.properties"
aws.ec2.routableCidrBlocks=10.0.0.0/8,192.168.100.0/24
aws.ec2.filter.tag.chaosTestTag=chaosTesting
aws.ec2.filter.keyName=chaosTestKey
aws.ec2.privateSshKeys.chaosTestKey=-----BEGIN RSA PRIVATE KEY-----\
MIIEpgIBAAKCAQEAuENqLqKYT7vld6EvSK1myOH29dX2lb3sLEXcHybgKGr1kjjU\
...
cxsbp/4QHL+kwxzKqF6w3s6ZQ5sOh8vBoUf3RhdjM7NY7dQOHhUltfN6\
-----END RSA PRIVATE KEY-----
```

### Environment Variables

Spring will read all environment variables to populate variables. Due to restricted characters in environment variables, you will need to refer to [Relaxed Bindings](#relaxed-bindings) to create equivalent variable names for use in shell.

### Vault

See [Vault Integration](./vault_integration.md) for more information.

## Relaxed Bindings

Reference: <https://github.com/spring-projects/spring-boot/wiki/relaxed-binding-2.0>

Spring Framework allows for multiple naming conventions to be used to bind a configuration into a single variable in the underlying Java code. Depending on where you are configuring the property from, certain characters may be unavailable, forcing you to use another naming convention.

### Examples

#### Multi-word property name

You can configure the AWS Module using the properties `aws.secret-access-key` and `aws.access-key-id`. In addition, you could use camelCaps (`aws.secretAccessKey` and `aws.accessKeyId`) or CAPS_AND_UNDERSCORES (`AWS_SECRET_ACCESS_KEY` and `AWS_ACCESS_KEY_ID`) to configure these values.

#### Arrays

If you are configuring users for HTTP Authentication, you would add them under `chaos.security.users[0].username` *et al*. To use CAPS_AND_UNDERSCORES, simply treat the array index as its own word (i.e., `CHAOS_SECURITY_USERS_0_USERNAME`). 