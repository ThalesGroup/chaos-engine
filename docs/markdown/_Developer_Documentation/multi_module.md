# Multi Module Structure

This page is intended to be a guide on how to create, maintain, and organize modules for components of the Chaos Engine.

## Module Structure

```
`--pom.xml (Parent)
 `-Module-A+-src
 |         `-pom.xml (Module A)
 `-Module-B+-src
           `-pom.xml (Module B)
```

The parent project directory contains a single pom.xml file that manages the entire project. The packaging field of that file will be pom. Each module is listed as a module under a modules section.

**Parent POM**
```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
...
    <modules>
        <module>Module-A</module>
        <module>Module-B</module>
    </modules>
    <packaging>pom</packaging>
...
</project>
```

Each module exists as a directory, exactly as named in the module field. That directory needs to contain a pom.xml in its root. Those pom.xml files may reference the project pom as their parent.

```xml tab="Example Module"
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <parent>
        <artifactId>parent-artifact</artifactId>
        <groupId>parent.group</groupId>
        <version>1.0.0-SNAPSHOT</version>
    </parent>
    <modelVersion>4.0.0</modelVersion>

    <artifactId>Module-B</artifactId>
    <packaging>jar</packaging>

    <dependencies>
        <dependency>
            <groupId>parent.group</groupId>
            <artifactId>Module-A</artifactId>
            <version>${project.version}</version>
            <scope>provided</scope>
        </dependency>
    </dependencies>

</project>
```

The child pom may reference other modules as dependencies. If this is done, they should be set with scope of provided. This ensures that the build process does not include the dependent JAR inside the child JAR.

### Nested Modules

To keep the root level clean, similar module types should be grouped together with a second level module. This prevents the need to regularly add updates to the project pom. This second level module will be of type **packaging** = **pom**, and be declared as a module of the parent.

```
chaosengine
|-- chaosengine-core
|   |-- pom.xml
|   `-- src
|       |-- main
|       `-- test
|-- chaosengine-experiments
|   |-- chaosengine-aws-ec2
|   |   |-- pom.xml
|   |   `-- src
|   |-- chaosengine-aws-rds
|   |   |-- pom.xml
|   |   `-- src
|   |-- chaosengine-kubernetes
|   |   |-- pom.xml
|   |   `-- src
|   |-- chaosengine-pcf
|   |   |-- pom.xml
|   |   `-- src
|   `-- pom.xml
|-- chaosengine-launcher
|   |-- pom.xml
|   `-- src
|       |-- main
|       `-- test
|-- chaosengine-notifications
|   |-- chaosengine-notif-datadog
|   |   |-- pom.xml
|   |   `-- src
|   |-- chaosengine-notif-slack
|   |   |-- pom.xml
|   |   `-- src
|   `-- pom.xml
`-- chaosengine-schedule
    |-- canada
    |   |-- pom.xml
    |   `-- src
    |-- czech-republic
    |   |-- pom.xml
    |   `-- src
    |-- france
    |   |-- pom.xml
    |   `-- src
    `-- pom.xml
```

## Building Modules

A successful local build of the entire project will require running the command **mvn install**. This will trigger a few things.

First, the Maven Reactor will determine the *necessary order* for building modules. The initial order will be the order of the modules in the project pom, but any dependent modules are pushed until after their dependencies are resolved. Some dependencies may not be obvious, so the modules should be maintained in a logical build order if possible.

Second, the project pom has been configured so that the **clean** goal is automatically run when the builder initializes for each step. This ensures that you cannot possibly work with cached builds of the project, as a cached version of a dependent layer may either cause or hide a bug in a further layer.

Third, each module will go through its build process, one at a time, including unit tests, and installing to the Maven Repository. This last step is necessary so that dependencies labelled as **scope = provided** will have their dependencies fulfilled.

If any module fails at any step, the remainder of steps will be skipped. For instance, a unit test failure in the Core module prevents the Experiment modules from being compiled.

A final output will show the results and build times of each module the reactor built, in the order they were built, with their corresponding exit status and build time.

**Final Build Output**

```
[INFO] ------------------------------------------------------------------------
[INFO] Reactor Summary for chaos-engine 1.1.0-SNAPSHOT:
[INFO]
[INFO] chaos-engine ....................................... SUCCESS [  3.103 s]
[INFO] chaosengine-launcher ............................... SUCCESS [  5.987 s]
[INFO] chaosengine-core ................................... SUCCESS [ 58.850 s]
[INFO] chaosengine-kubernetes ............................. SUCCESS [ 16.017 s]
[INFO] chaosengine-aws-ec2 ................................ SUCCESS [  9.143 s]
[INFO] chaosengine-aws-rds ................................ SUCCESS [  8.887 s]
[INFO] chaosengine-pcf .................................... SUCCESS [ 10.948 s]
[INFO] ------------------------------------------------------------------------
[INFO] BUILD SUCCESS
[INFO] ------------------------------------------------------------------------
[INFO] Total time:  01:54 min
[INFO] Finished at: 2019-05-01T13:48:34-04:00
[INFO] ------------------------------------------------------------------------
```

### Packaging and Assembly

#### Launcher

The Launcher module requires special packaging, since it has the Main method in it. This is accomplished with the **spring-boot-maven-plugin** repackage. This is run by default as part of inheriting the **spring-boot-starter-parent**, but causes excessive and unnecessary file sizes in other modules. As such, in the project pom file, the plugin is set with **configuration.skip=true**, and then explicitly configured in the Launcher module pom. The launcher module is configured to create two copies of its Jar, as the repackaging will cause it to fail being consumed as a dependency in further layers.

The original version of the launcher Jar is unchanged. The version run through the **spring-boot-maven-plugin** is placed in the **target/entrypoint** directory. This is used to identify it in the Dockerfile and choose the starting Jar for the image to run.

#### Experiment Modules

The experiment modules have dependencies for SDKs used explicitly in their module. To keep the ability to load these as needed, the modules use the **maven-assembly-plugin** configured in the project pom\'s **pluginManagement** section. This plugin requires that the module calling it have a file called **src/assembly/bin.xml** in their module folder. The assembler instructions should use an **id** of **assembler**, so that the folder structure is common, and a **format** of **dir**. Then, in the **dependencySets**, include the specific **groupId:artifactId** of any packages imported in the pom.xml, and set **useTransitiveDependencies** and **useTransitiveFiltering** to **true**. This will cause all required JARs, both directly and indirectly, to be assembled in a subdirectory of the assembled JAR file. This is used in the Dockerfile to side load those JARs in the ClassPath, keeping the size of the modules to a minimum.

### Docker Build

The Dockerfile has been assembled to account for the build instructions of the engine. A **.dockerignore** file automatically prunes out **\*\*/target/** folders, as well as non-code files that are created by IDE\'s, such as **.iml** files.

All module folders are copied into the Docker image, preserving their directory structure.

The launcher module is run through a **mvn install** before the remainder of the modules are copied in. This will cache the majority of the dependencies, making future builds faster.

In the final image, all Jar files that end up in the **target** directories are copied into the **lib** folder, as well as all Jar files that are in **target/\*-assembler** directories. It is expected to find exactly one Jar in **chaosengine-\*/target/entrypoint/\*.jar** from the Launcher module, and places that file as **chaosengine.jar** in the working directory. That Jar is called as the entrypoint.

The Docker image entrypoint is configured to launch the **chaosengine.jar**, while including all Jar files in the **lib** folder in its classpath. Those libraries are also imported into Spring via the **loader.path** property to scan for Spring Bean instantiation.

## Common Test Resources

In order to share common test resources (such as the **application.properties** or **logback-test.xml** configuration files), a module titled **chaosengine-test-utilities** exists. This module does **NOT** use the project module as a parent, and is instead independent. It is imported in the project pom with the scope of **test**. This maps all code and resources in the test utilities **src** folder into the classpath when running unit tests. This allows us to hold a single **logback-test.xml** file, plus any other files we will need to share between modules in testing.
