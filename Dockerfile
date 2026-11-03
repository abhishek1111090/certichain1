# Builds and runs the CertiChain web server in a container (used for the public deployment).
#   docker build -t certichain .
#   docker run -p 8080:8080 -e ADMIN_PASSWORD=... -e REGISTRAR_PASSWORD=... certichain

FROM eclipse-temurin:21-jdk AS build
WORKDIR /src
# Download dependencies first so code changes do not re-download them
COPY mvnw pom.xml ./
COPY .mvn .mvn
COPY core/pom.xml core/pom.xml
COPY server/pom.xml server/pom.xml
COPY desktop/pom.xml desktop/pom.xml
RUN sh mvnw -q -B -pl server -am dependency:go-offline -DskipTests || true
COPY core core
COPY server server
RUN sh mvnw -q -B -pl server -am package -DskipTests

FROM eclipse-temurin:21-jre
WORKDIR /app
RUN useradd --create-home certichain
USER certichain
COPY --from=build /src/server/target/certichain-server.war app.war
# Fits the 512 MB free tiers of most hosts
ENV JAVA_OPTS="-XX:MaxRAMPercentage=70 -XX:+UseSerialGC -Xss512k"
EXPOSE 8080
CMD ["sh", "-c", "exec java $JAVA_OPTS -jar app.war"]
