
# Stage 1: Build the application
FROM eclipse-temurin:21-jdk AS build

WORKDIR /app

COPY . .

RUN chmod +x mvnw && ./mvnw -DskipTests package

# Stage 2: Run the application
FROM eclipse-temurin:21-jre

WORKDIR /app

# Create a dedicated non-root user
RUN groupadd --system appgroup && \
    useradd --system --gid appgroup --home-dir /app appuser

# Copy the application JAR with correct ownership
COPY --from=build --chown=appuser:appgroup /app/target/*.jar app.jar

# Switch to the non-root user
USER appuser

EXPOSE 8081

ENTRYPOINT ["java", "-jar", "app.jar"]
