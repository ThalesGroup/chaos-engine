FROM maven:3.6-jdk-11-slim AS build-env
WORKDIR /chaosengine
COPY pom.xml ./
COPY chaosengine-launcher/pom.xml ./chaosengine-launcher/
RUN cd chaosengine-launcher && \
    mvn dependency:go-offline install -Dsilent=true && \
    cd ..
COPY chaosengine-core/pom.xml ./chaosengine-core/
COPY chaosengine-kubernetes/pom.xml ./chaosengine-kubernetes/
COPY chaosengine-aws-ec2/pom.xml ./chaosengine-aws-ec2/
COPY chaosengine-aws-rds/pom.xml ./chaosengine-aws-rds/
COPY chaosengine-pcf/pom.xml ./chaosengine-pcf/
COPY chaosengine-notif-slack/pom.xml ./chaosengine-notif-slack/
COPY chaosengine-notif-datadog/pom.xml ./chaosengine-notif-datadog/
COPY chaosengine-schedule ./chaosengine-schedule/

#RUN mvn dependency:go-offline -Dsilent=true
COPY chaosengine-launcher/src/ ./chaosengine-launcher/src/
COPY chaosengine-core/src/ ./chaosengine-core/src/
COPY chaosengine-kubernetes/src/ ./chaosengine-kubernetes/src/
COPY chaosengine-aws-ec2/src/ ./chaosengine-aws-ec2/src/
COPY chaosengine-aws-rds/src/ ./chaosengine-aws-rds/src/
COPY chaosengine-pcf/src/ ./chaosengine-pcf/src/
COPY chaosengine-notif-slack/src/ ./chaosengine-notif-slack/src/
COPY chaosengine-notif-datadog/src/ ./chaosengine-notif-datadog/src/

RUN mvn install

FROM openjdk:11-jre-slim AS develop
EXPOSE 8080
WORKDIR /chaosengine
COPY --from=build-env /chaosengine/*/target/*.jar /chaosengine/*/*/target/*.jar ./lib/
COPY --from=build-env /chaosengine/*/target/*-assembler/*.jar ./lib/
COPY --from=build-env /chaosengine/*/target/entrypoint/*.jar ./chaosengine.jar
RUN rm ./lib/chaosengine-launcher*.jar
ENV DEPLOYMENT_ENVIRONMENT=DEVELOPMENT
LABEL com.datadoghq.ad.logs="[ { \"source\":\"java\", \"service\": \"chaosengine\" } ]"
ENTRYPOINT ["java", "-Djava.security.egd=file:/dev/./urandom","-classpath", ".:./lib/*", "-Dloader.path=lib", "-jar", "chaosengine.jar"]

FROM develop AS debug
ENTRYPOINT ["java", "-Djava.security.egd=file:/dev/./urandom", "-agentlib:jdwp=transport=dt_socket,server=y,suspend=y,address=5005", "-jar", "/chaosengine.jar"]
EXPOSE 5005

FROM develop AS master
ENV DEPLOYMENT_ENVIRONMENT=PROD