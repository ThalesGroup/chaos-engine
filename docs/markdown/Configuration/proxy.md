# Proxy Configuration

Chaos Engine can use Java's built-in proxy support for both HTTP and HTTPS proxies. This is accomplished by adding additional parameters to the docker *command* parameter for the Chaos Engine instance.

The built-in properties that need to be set are **http.proxyHost**, **http.proxyPort**, **http.nonProxyHosts**, **https.proxyHost**, and **https.proxyPort**.

Learn more about how Java uses proxies [here](https://docs.oracle.com/javase/8/docs/technotes/guides/net/proxies.html).

## Chaos Engine Startup Parameters

The Chaos Engine Dockerfile splits the startup instructions for Chaos Engine using both the *Entrypoint* and *Command* sections. It is possible to append new Java Properties by modifying the command entry, but the original information still needs to be preserved at the end of the command.

The existing command entry is **[ "-jar", "chaosengine.jar" ]**. Properties need to be added before this section. 

```yaml tab="Sample docker-compose"
  chaosengine:
    command:
      - "-Dhttp.proxyHost=proxy"
      - "-Dhttp.proxyPort=8080"
      - "-Dhttps.proxyHost=proxy"
      - "-Dhttps.proxyPort=8080"
      - "-jar"
      - "chaosengine.jar"
```

## HTTPS Proxy Certificates

The Java Keystore that is built in to the OpenJDK Base Image should support most major certificate vendors. If you have an HTTPS Proxy that is untrusted, you can add the CA Certificate to the Java KeyStore.

Instructions on how to load a key into the keystore can be found [here](https://docs.oracle.com/javase/tutorial/security/toolfilex/rstep1.html).

The best mechanism for loading the key into the keystore is to use a sidecar container from the same base image (openjdk:11-jre-slim). The keystore directory is located in */usr/local/openjdk-11/lib/security*. For a specific example, please see the *docker-compose-mitmproxy.yml* configuration used for testing in the project directory.