# Stage 1: Compile and Package the application
FROM maven:3.9.6-eclipse-temurin-21-alpine AS build
WORKDIR /app

# Copy the build configuration and source code
COPY pom.xml .
COPY src ./src

# Build the application, skipping tests to accelerate local deployment
RUN mvn clean package -DskipTests

# Stage 2: Create the runtime image
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Copy the built jar from the first stage
COPY --from=build /app/target/*.jar app.jar

# Expose the port the Spring Boot server uses
EXPOSE 8080

# Execute the application
ENTRYPOINT ["java", "-jar", "app.jar"]