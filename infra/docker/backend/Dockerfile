# syntax=docker/dockerfile:1.7

FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /workspace/backend

ENV MAVEN_OPTS="-Djdk.tls.client.protocols=TLSv1.2,TLSv1.3 -Dhttps.protocols=TLSv1.2,TLSv1.3"

COPY backend/pom.xml backend/mvnw ./
COPY backend/.mvn .mvn
COPY database/migrations /workspace/database/migrations

RUN chmod +x mvnw

RUN --mount=type=cache,target=/root/.m2 \
    sh -ec 'for attempt in 1 2 3; do \
      ./mvnw -B -q -Dmaven.test.skip=true dependency:go-offline && exit 0; \
      echo "Maven dependency prefetch failed on attempt ${attempt}, retrying..." >&2; \
      sleep $((attempt * 5)); \
    done; \
    exit 1'

COPY backend/src src

RUN --mount=type=cache,target=/root/.m2 \
    sh -ec 'for attempt in 1 2 3; do \
      ./mvnw -B -q package && exit 0; \
      echo "Maven package failed on attempt ${attempt}, retrying..." >&2; \
      sleep $((attempt * 5)); \
    done; \
    exit 1'

FROM eclipse-temurin:21-jre
WORKDIR /app

RUN apt-get update \
    && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/*

COPY --from=build /workspace/backend/target/geoanalytics-dashboard-backend-0.1.0-SNAPSHOT.jar /app/app.jar

ENV JAVA_OPTS=""

EXPOSE 8080

ENTRYPOINT ["sh","-c","exec java $JAVA_OPTS -jar /app/app.jar"]
