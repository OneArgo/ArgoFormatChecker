FROM eclipse-temurin:21-jdk-jammy AS builder

LABEL org.opencontainers.image.authors="yvan.lubac@euro-argo.eu"
LABEL org.opencontainers.image.description="Docker Image for Argo netcdf File Checler"
LABEL org.opencontainers.image.url="https://github.com/OneArgo/ArgoFormatChecker"
LABEL org.opencontainers.image.source="https://github.com/OneArgo/ArgoFormatChecker"
LABEL org.opencontainers.image.documentation="https://github.com/OneArgo/ArgoFormatChecker"
LABEL org.opencontainers.image.licenses="TBD"
LABEL org.opencontainers.image.vendor="OneArgo"


WORKDIR /build

COPY ./.mvn ./.mvn
COPY ./mvnw ./mvnw
COPY ./pom.xml ./pom.xml
COPY ./src ./src

# Ensure mvnw has execution permissions
RUN chmod +x ./mvnw \
&& ./mvnw clean package

FROM eclipse-temurin:8-jre-jammy AS runtime

WORKDIR /app

RUN set -e \ 
&& addgroup --system --gid 1001 gcontainer \
&& adduser --system --uid 1001 -G gcontainer fileCheckerRunner

RUN set -e \
&& mkdir -p /app/results /app/data /app/file_checker_spec \
&& chown root:gcontainer /app/results /app/data /app/file_checker_spec \
&& chmod 2775 /app/results \
&& chmod 2755 /app/data /app/file_checker_spec

# Copy the built jar file to the runtime stage
COPY --from=builder --chown=root:gcontainer --chmod=750 /build/target/file_checker*.jar app.jar
# Specify the default command for running the app
ENTRYPOINT ["java", "-jar", "/app/app.jar"]

USER fileCheckerRunner