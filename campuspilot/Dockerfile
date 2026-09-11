# 构建阶段
FROM maven:3.9-eclipse-temurin-21 AS builder

WORKDIR /app

COPY pom.xml .
COPY docker ./docker
RUN mvn -s docker/settings.xml dependency:go-offline -q

COPY src ./src
RUN mvn -s docker/settings.xml clean package -DskipTests -q

# 运行阶段
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

COPY --from=builder /app/target/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]