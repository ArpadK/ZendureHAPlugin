# Stage 1: Build
FROM eclipse-temurin:25-jdk-alpine AS build
WORKDIR /app
RUN apk add --no-cache maven
COPY pom.xml .
RUN mvn dependency:go-offline -q
COPY src ./src
RUN mvn clean package -DskipTests -q

# Stage 2: Runtime
FROM eclipse-temurin:25-jre-alpine
WORKDIR /app
COPY --from=build /app/target/zendure-ha-plugin-0.1.0-SNAPSHOT.jar app.jar
COPY run.sh run.sh
RUN chmod +x run.sh
EXPOSE 8099
ENTRYPOINT ["/app/run.sh"]
