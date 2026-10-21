FROM eclipse-temurin:25-jdk-noble AS build
WORKDIR /src
COPY gradlew ./
COPY gradle gradle
COPY server/docker/settings.gradle.kts settings.gradle.kts
COPY server server
RUN ./gradlew --no-daemon -Dorg.gradle.jvmargs=-Xmx1g -Pkotlin.compiler.execution.strategy=in-process :server:installDist

FROM eclipse-temurin:25-jre-noble
RUN useradd --system --uid 10001 outdate
COPY --from=build /src/server/build/install/server /opt/outdate
USER outdate
EXPOSE 8080
ENTRYPOINT ["/opt/outdate/bin/server"]
