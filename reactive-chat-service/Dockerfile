FROM eclipse-temurin:17-jdk AS builder

WORKDIR /workspace

COPY gradle ./gradle
COPY gradlew settings.gradle.kts build.gradle.kts gradle.properties ./
COPY platform-dependencies ./platform-dependencies
COPY reactive-chat-service ./reactive-chat-service

RUN ./gradlew :reactive-chat-service:bootJar --no-daemon

FROM eclipse-temurin:17-jre

WORKDIR /app

RUN apt-get update \
  && apt-get install -y --no-install-recommends curl \
  && rm -rf /var/lib/apt/lists/*

COPY --from=builder /workspace/reactive-chat-service/build/libs/*.jar /app/reactive-chat-service.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/reactive-chat-service.jar"]
