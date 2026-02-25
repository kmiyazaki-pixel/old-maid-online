# 1. ビルド環境 (Maven + Amazon Corretto 17)
FROM maven:3.9.6-amazoncorretto-17 AS build
COPY . .
RUN mvn clean package -DskipTests

# 2. 実行環境 (Amazon Corretto 17)
FROM amazoncorretto:17-al2023-jdk
COPY --from=build /target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java","-jar","/app.jar"]
