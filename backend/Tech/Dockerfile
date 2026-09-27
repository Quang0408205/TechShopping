# Backend image for docker compose (dev). Build: Maven + JDK 21, run: JRE 21.
# The Maven repository is kept in a BuildKit cache, so rebuilds only download new dependencies.

FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
RUN --mount=type=cache,target=/root/.m2 mvn -q -B dependency:go-offline
COPY src ./src
RUN --mount=type=cache,target=/root/.m2 mvn -q -B -DskipTests package \
    && cp target/Tech-*.jar /app/app.jar

FROM eclipse-temurin:21-jre
ENV TZ=Asia/Ho_Chi_Minh
RUN useradd --system --no-create-home app
WORKDIR /app
COPY --from=build /app/app.jar app.jar
USER app
EXPOSE 8080
ENTRYPOINT ["java", "-Duser.timezone=Asia/Ho_Chi_Minh", "-jar", "/app/app.jar"]
