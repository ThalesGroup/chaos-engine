FROM maven:3.6-jdk-11-slim AS build-env
WORKDIR /chaosengine
COPY pom.xml ./
COPY chaosengine-launcher/pom.xml ./chaosengine-launcher/
COPY chaosengine-core/pom.xml ./chaosengine-core/
COPY chaosengine-kubernetes/pom.xml ./chaosengine-kubernetes/
#RUN mvn dependency:go-offline -Dsilent=true
COPY chaosengine-launcher/src/ ./chaosengine-launcher/src/
COPY chaosengine-core/src/ ./chaosengine-core/src/
COPY chaosengine-kubernetes/src/ ./chaosengine-kubernetes/src/
RUN mvn package

FROM openjdk:11-jre-slim AS develop
EXPOSE 8080
WORKDIR /chaosengine
COPY --from=build-env /chaosengine/*/target/*.jar ./lib/
RUN  mv ./lib/*-exec.jar ./chaosengine.jar
ENV DEPLOYMENT_ENVIRONMENT=DEVELOPMENT
LABEL com.datadoghq.ad.logs="[ { \"source\":\"java\", \"service\": \"chaosengine\" } ]"
ENTRYPOINT ["java", "-Djava.security.egd=file:/dev/./urandom","-classpath", ".:./lib/*", "-Dloader.path=lib", "-jar", "chaosengine.jar"]

FROM develop AS debug
ENTRYPOINT ["java", "-Djava.security.egd=file:/dev/./urandom", "-agentlib:jdwp=transport=dt_socket,server=y,suspend=y,address=5005", "-jar", "/chaosengine.jar"]
EXPOSE 5005

FROM develop AS master
ENV DEPLOYMENT_ENVIRONMENT=PROD