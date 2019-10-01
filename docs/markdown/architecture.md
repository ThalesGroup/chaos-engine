# Architecture

|  Application Information |  |
| --- | --- |
|  Language   | Java 11 |
| Operating System | Cross Platform |
| Platform | Java Virtual Machine |
| Frameworks | Spring Framework <br/> Spring Boot 2.1.4.RELEASE |
| Port Requirements | API Interface: 8080 <br/> Management Interface: 8081 |


## Runtime Dependencies  
| Docker | |
| --- | --- |
| Minimum Version | 18 |
| Recommended Version | 18.09.7+ |
| Link | [Docker] |

| HashiCorp Vault | (Optional) |
| --- | --- |
| Minimum Version | 0.9.x |
| Recommended Version | 0.10.x |
| Link | [Vault by HashiCorp] |
| Docker Image | [Vault - Official Docker Image] |

| DataDog Agent | (Optional) |
| --- | --- |
| Docker Image | [DataDog Agent for Docker] |

## Development Dependencies
| Java | |
| --- | --- |
| Minimum Version | 11 |
| Runtime Environment | [Java SE Runtime Environment] |
| Development Kit | [JDK 11 from OpenJDK] |

| Maven | |
| --- | --- |
| Minimum Version | 3.6.0 |
| Recommended Version | 3.6.1 |
| Link | [Apache Maven Project] |

| MkDocs | |
| --- | --- |
| Minimum Version | 1.0.4 |
| Link | [MkDocs] |

| Docker Compose | |
| --- | --- |
| Minimum Version | 1.22.0 |
| Link | [Install Docker Compose] |

!!! tip
    The Linux Installation steps for **docker-compose** instruct you to extract the file into **/usr/local/bin**. In some flavours of Linux, a copy of docker-compose may already exist in another path. If you seem to still have an old version after running the installation, try running *which docker-compose* to verify you are executing the version you just downloaded.

[Java SE Runtime Environment]: https://www.oracle.com/technetwork/java/javase/downloads/jdk11-downloads-5066655.html
[JDK 11 from OpenJDK]: https://openjdk.java.net/projects/jdk/11/
[Vault by HashiCorp]: https://www.vaultproject.io/
[Apache Maven Project]: https://maven.apache.org
[Vault - Official Docker Image]: https://hub.docker.com/_/vault
[Docker]: https://www.docker.com/
[DataDog Agent for Docker]: https://docs.datadoghq.com/agent/docker/?tab=standard
[Install Docker Compose]: https://docs.docker.com/compose/install/
[MkDocs]: https://www.mkdocs.org/