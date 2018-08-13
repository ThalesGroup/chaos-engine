FROM maven:3.5.4-jdk-8-alpine AS build-env
WORKDIR /chaosengine
ADD pom.xml /chaosengine
RUN mvn verify dependency:resolve --fail-never
ADD . /chaosengine
RUN cd /chaosengine && mvn clean test package

FROM openjdk:8-jdk-alpine
EXPOSE 8080
COPY --from=build-env /chaosengine/target/chaosengine.jar /
ENTRYPOINT ["java","-Djava.security.egd=file:/dev/./urandom","-jar","/chaosengine.jar"]
