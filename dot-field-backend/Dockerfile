# ──────────────────────────────────────────────────────────────
# DOT Field — Backend Multi-Stage Dockerfile
# ──────────────────────────────────────────────────────────────

# ── Stage 1: Build ───────────────────────────────────────────
FROM eclipse-temurin:21-jdk-jammy AS build
WORKDIR /workspace/dot-field-backend

# Copy Maven wrapper and POM first for layer caching
COPY mvnw .
COPY .mvn .mvn
COPY pom.xml .

RUN ./mvnw dependency:go-offline -B

# Copy backend source code
COPY src src

# Build application JAR skipping tests (tests executed in CI)
RUN ./mvnw clean package -DskipTests

# ── Stage 2: Runtime ──────────────────────────────────────────
FROM eclipse-temurin:21-jre-jammy
WORKDIR /app

RUN addgroup --system spring && adduser --system --ingroup spring spring
USER spring:spring

COPY --from=build /workspace/dot-field-backend/target/*.jar app.jar

ENV SPRING_PROFILES_ACTIVE=prod
EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
