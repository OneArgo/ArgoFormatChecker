FROM eclipse-temurin:21-jdk-alpine AS builder

WORKDIR /build

COPY ./.mvn ./.mvn
COPY ./mvnw ./mvnw
COPY ./pom.xml ./pom.xml
COPY ./src ./src

# Ensure mvnw has execution permissions
RUN chmod +x ./mvnw \
&& ./mvnw clean package

FROM eclipse-temurin:8-jre-alpine AS runtime

WORKDIR /app

RUN set -e \ 
&& addgroup --system --gid 1001 gcontainer \
&& adduser --system --uid 1001 -G gcontainer fileCheckerRunner

# Copy the built jar file to the runtime stage
COPY --from=builder --chown=root:gcontainer --chmod=750 /build/target/file_checker*.jar app.jar
# Specify the default command for running the app
ENTRYPOINT ["java", "-jar", "/app/app.jar"]

USER fileCheckerRunner