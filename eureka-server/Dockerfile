# syntax=docker/dockerfile:1
# Build from the repository root (the module is built together with the parent pom):
#   docker build -f eureka-server/Dockerfile -t saschoolsport/eureka-server .

# ---- build ----
FROM maven:3.9-eclipse-temurin-25 AS build
WORKDIR /workspace
COPY . .
RUN --mount=type=cache,target=/root/.m2 \
    mvn -B -ntp -q -pl eureka-server -am -DskipTests package \
 && java -Djarmode=tools -jar eureka-server/target/eureka-server-*.jar \
      extract --layers --launcher --destination /workspace/extracted

# ---- runtime ----
FROM eclipse-temurin:25-jre
# curl is only needed for the container healthcheck (the JRE image does not ship it)
RUN apt-get update && apt-get install -y --no-install-recommends curl \
 && rm -rf /var/lib/apt/lists/* \
 && groupadd --system app && useradd --system --gid app --no-create-home app
WORKDIR /app
# layers ordered from the least to the most frequently changing -> better image cache reuse
COPY --from=build --chown=app:app /workspace/extracted/dependencies/ ./
COPY --from=build --chown=app:app /workspace/extracted/spring-boot-loader/ ./
COPY --from=build --chown=app:app /workspace/extracted/snapshot-dependencies/ ./
COPY --from=build --chown=app:app /workspace/extracted/application/ ./
USER app
EXPOSE 8761
ENV JAVA_OPTS="-XX:MaxRAMPercentage=75 -XX:+ExitOnOutOfMemoryError"
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS org.springframework.boot.loader.launch.JarLauncher"]
