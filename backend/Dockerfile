# Build context is the REPOSITORY ROOT (the SQL migrations in /database are packaged into the jar):
#   docker build -f backend/Dockerfile .
# ---------- dependencies (cached until pom.xml changes) ----------
FROM maven:3.9-eclipse-temurin-21 AS deps
WORKDIR /build
COPY backend/pom.xml backend/pom.xml
COPY database database
RUN mvn -f backend/pom.xml -B -q dependency:go-offline

# ---------- build ----------
FROM deps AS build
COPY backend/src backend/src
# Tests are run separately (see README "Testing"); the image build stays fast and deterministic.
RUN mvn -f backend/pom.xml -B -q package -DskipTests

# ---------- run ----------
FROM eclipse-temurin:21-jre
RUN apt-get update && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/* \
    && useradd --system --uid 10001 --create-home lms
WORKDIR /app
COPY --from=build /build/backend/target/library-management-system-*.jar app.jar
USER lms
EXPOSE 8080
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75"
HEALTHCHECK --interval=15s --timeout=5s --start-period=60s --retries=5 \
  CMD curl -fsS http://localhost:8080/actuator/health || exit 1
ENTRYPOINT ["java", "-jar", "app.jar"]
