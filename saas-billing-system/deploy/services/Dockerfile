FROM eclipse-temurin:17-jdk AS build

WORKDIR /workspace

COPY . .

ARG MODULE
RUN test -n "$MODULE"
RUN ./gradlew "$MODULE:bootJar" --no-daemon
RUN MODULE_PATH="$(echo "$MODULE" | sed 's/^://; s/:/\//g')" && \
    JAR_PATH="$(find "$MODULE_PATH/build/libs" -maxdepth 1 -type f -name '*.jar' ! -name '*-plain.jar' -print -quit)" && \
    test -n "$JAR_PATH" && \
    mkdir -p /app && \
    cp "$JAR_PATH" /app/app.jar

FROM eclipse-temurin:17-jre

WORKDIR /app

COPY --from=build /app/app.jar /app/app.jar

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
