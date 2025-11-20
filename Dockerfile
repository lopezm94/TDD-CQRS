FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# Copy the built jar from target directory
COPY target/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
