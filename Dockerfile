FROM openjdk:23-jdk-slim

# Set the working directory
WORKDIR /infra-app

# Specify the build version as a build argument
ARG BUILD_VERSION=0.0.1

# Copy the built JAR file into the image
COPY target/infra-app-${BUILD_VERSION}-SNAPSHOT.jar service.jar

# Expose the application port (e.g., 8080)
EXPOSE 8080

# Define the command to run the JAR
ENTRYPOINT ["java", "-jar", "service.jar"]
