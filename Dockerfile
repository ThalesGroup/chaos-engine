FROM python:3-alpine AS build-docs
WORKDIR /mkdocs
COPY ci/docs/mkdocs_requirements.txt requirements.txt
RUN pip install -r requirements.txt
COPY docs/ docs/
RUN mkdocs build --config-file docs/mkdocs.yml --site-dir help/

FROM maven:3.6-jdk-11-slim AS build-env
WORKDIR /chaosengine
COPY pom.xml ./
COPY chaosengine-launcher/pom.xml ./chaosengine-launcher/
COPY chaosengine-test-utilities ./chaosengine-test-utilities/
RUN mvn -B -f ./chaosengine-test-utilities/pom.xml dependency:go-offline -Dsilent install
RUN mvn -B -f ./chaosengine-launcher/pom.xml dependency:go-offline -Dsilent install
COPY chaosengine-core/pom.xml ./chaosengine-core/
COPY chaosengine-schedule ./chaosengine-schedule/
COPY chaosengine-notifications ./chaosengine-notifications/
COPY chaosengine-experiments ./chaosengine-experiments/

#RUN mvn dependency:go-offline -Dsilent=true
COPY chaosengine-launcher/src/ ./chaosengine-launcher/src/
COPY chaosengine-core/src/ ./chaosengine-core/src/
COPY --from=build-docs /mkdocs/docs/help ./chaosengine-launcher/src/main/resources/static/help/

ARG BUILD_VERSION
RUN if [ -n "${BUILD_VERSION}" ] ; then mvn -B versions:set -DnewVersion=${BUILD_VERSION} -DprocessAllModules ; fi

RUN mvn -B install && rm -rf chaosengine-test*

FROM openjdk:11.0.4-jre-slim AS develop
EXPOSE 8080
WORKDIR /chaosengine
COPY --from=build-env /chaosengine/*/target/*.jar /chaosengine/*/*/target/*.jar ./lib/
COPY --from=build-env /chaosengine/*/target/*-assembler/*.jar /chaosengine/*/*/target/*-assembler/*.jar ./lib/
COPY --from=build-env /chaosengine/*/target/entrypoint/*.jar ./chaosengine.jar
RUN rm ./lib/chaosengine-launcher*.jar
ENV DEPLOYMENT_ENVIRONMENT=DEVELOPMENT
ENV SPRING_PROFILES_ACTIVE=DEVELOPMENT
LABEL com.datadoghq.ad.logs='[ { "source":"java", "service": "chaosengine" } ]'
ENTRYPOINT ["java", "-Djava.security.egd=file:/dev/./urandom","-classpath", ".:./lib/*", "-Dloader.path=lib", "-jar", "chaosengine.jar"]

FROM develop AS debug
ENTRYPOINT ["java", "-Djava.security.egd=file:/dev/./urandom", "-agentlib:jdwp=transport=dt_socket,server=y,suspend=y,address=5005", "-jar", "/chaosengine.jar"]
EXPOSE 5005

FROM develop AS master
ENV DEPLOYMENT_ENVIRONMENT=PROD
ENV SPRING_PROFILES_ACTIVE=PRODUCTION