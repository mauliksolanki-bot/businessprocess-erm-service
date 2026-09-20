# =========================
# Stage 1: Build application
# =========================
FROM maven:3.9.9-eclipse-temurin-21 AS build

WORKDIR /app

# Copy Maven configuration first for better Docker layer caching
COPY pom.xml .

# Download dependencies
RUN mvn dependency:go-offline -B

# Copy source code
COPY src ./src

# Build Spring Boot JAR
RUN mvn clean package -DskipTests


# =========================
# Stage 2: Run application
# =========================
FROM eclipse-temurin:21-jre

WORKDIR /app

# Download the New Relic Java agent so JAVA_TOOL_OPTIONS can point at a real jar.
ADD https://download.newrelic.com/newrelic/java-agent/newrelic-agent/current/newrelic-java.zip /tmp/newrelic-java.zip
RUN mkdir -p /opt/newrelic \
    && jar xf /tmp/newrelic-java.zip \
    && cp newrelic/newrelic.jar /opt/newrelic/newrelic.jar \
    && cp newrelic/newrelic.yml /opt/newrelic/newrelic.yml \
    && rm -rf /tmp/newrelic-java.zip newrelic

# Copy generated JAR from build stage
COPY --from=build /app/target/*.jar app.jar

# Render/Vercel/cloud platforms provide PORT
ENV PORT=8080

EXPOSE 8080

# Start Spring Boot
# Set JAVA_TOOL_OPTIONS to include -javaagent:/path/to/newrelic.jar when the agent is mounted or baked in.
ENTRYPOINT ["sh", "-c", "java -jar app.jar --server.port=${PORT}"]