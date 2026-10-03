# syntax=docker/dockerfile:1

FROM maven:3.9.16-eclipse-temurin-21-noble@sha256:99e61abcff91a9b1333463bd8451fb18495d6eba9250ac66a338b518f8278320 AS build

WORKDIR /build
COPY pom.xml ./
RUN --mount=type=cache,id=tinyroute-maven,target=/root/.m2,sharing=locked \
    mvn --batch-mode --no-transfer-progress dependency:go-offline

COPY src/main ./src/main
# Integration tests need PostgreSQL and Redis; run Maven verify before image release.
RUN --mount=type=cache,id=tinyroute-maven,target=/root/.m2,sharing=locked \
    mvn --batch-mode --no-transfer-progress -Dmaven.test.skip=true package

FROM eclipse-temurin:21-jre-noble@sha256:22138efd69393501fccd8176ae16b01791ed71ff801b28f0359415389b17c766 AS runtime

RUN groupadd --gid 10001 tinyroute \
    && useradd --uid 10001 --gid tinyroute --no-create-home --shell /usr/sbin/nologin tinyroute

WORKDIR /app
COPY --from=build --chmod=0444 /build/target/tinyroute-backend-*.jar ./application.jar

USER 10001:10001
EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=5s --start-period=60s --retries=3 \
    CMD curl --fail --silent --show-error --max-time 3 --output /dev/null \
        "http://127.0.0.1:${SERVER_PORT:-8080}/actuator/health" || exit 1

ENTRYPOINT ["java", "-jar", "/app/application.jar"]
