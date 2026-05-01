# syntax=docker/dockerfile:1.7

ARG SERVICE_MODULE

FROM maven:3.9.9-eclipse-temurin-17 AS build
ARG SERVICE_MODULE
WORKDIR /workspace

ENV MAVEN_OPTS="-Djdk.tls.client.protocols=TLSv1.2,TLSv1.3 -Dhttps.protocols=TLSv1.2,TLSv1.3"

COPY services/pom.xml services/pom.xml
COPY services/mzhd-generator-common/pom.xml services/mzhd-generator-common/pom.xml
COPY services/mzhd-train-generator/pom.xml services/mzhd-train-generator/pom.xml
COPY services/mzhd-event-generator/pom.xml services/mzhd-event-generator/pom.xml
COPY services/mzhd-personnel-generator/pom.xml services/mzhd-personnel-generator/pom.xml

RUN --mount=type=cache,target=/root/.m2 \
    sh -ec 'for attempt in 1 2 3; do \
      mvn -B -q -f services/pom.xml -pl ${SERVICE_MODULE} -am -Dmaven.test.skip=true dependency:go-offline && exit 0; \
      echo "Generator Maven dependency prefetch failed on attempt ${attempt}, retrying..." >&2; \
      sleep $((attempt * 5)); \
    done; \
    exit 1'

COPY services services
RUN --mount=type=cache,target=/root/.m2 \
    sh -ec 'for attempt in 1 2 3; do \
      mvn -B -q -f services/pom.xml -pl ${SERVICE_MODULE} -am -Dmaven.test.skip=true package && exit 0; \
      echo "Generator Maven package failed on attempt ${attempt}, retrying..." >&2; \
      sleep $((attempt * 5)); \
    done; \
    exit 1'

FROM eclipse-temurin:17-jre
ARG SERVICE_MODULE
WORKDIR /app

COPY --from=build /workspace/services/${SERVICE_MODULE}/target/${SERVICE_MODULE}-0.1.0-SNAPSHOT.jar /app/app.jar

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
