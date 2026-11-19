FROM eclipse-temurin:25-jre

WORKDIR /app

COPY target/lost-found-app-1.0.jar app.jar

EXPOSE 8080

CMD ["java", "-jar", "app.jar"]
