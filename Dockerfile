FROM maven:3.6-jdk-11-slim AS build-env
WORKDIR /chaosengine
ADD pom.xml /chaosengine
RUN mvn dependency:go-offline -Dsilent=true
ADD src /chaosengine/src
RUN cd /chaosengine && mvn clean test package

FROM openjdk:11-jre-slim AS develop
EXPOSE 8080
COPY --from=build-env /chaosengine/target/chaosengine.jar /
ENV DEPLOYMENT_ENVIRONMENT=DEVELOPMENT
LABEL com.datadoghq.ad.logs="[ { \"source\":\"java\", \"service\": \"chaosengine\" } ]"
ENTRYPOINT ["java","-Djava.security.egd=file:/dev/./urandom","-jar","/chaosengine.jar"]

FROM develop AS debug
ENTRYPOINT ["java", "-Djava.security.egd=file:/dev/./urandom", "-agentlib:jdwp=transport=dt_socket,server=y,suspend=y,address=5005", "-jar", "/chaosengine.jar"]
EXPOSE 5005

FROM develop AS master
ENV DEPLOYMENT_ENVIRONMENT=PROD