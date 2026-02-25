# 1. ビルド環境 (軽量なAlphine版に変更)
FROM maven:3.8.5-openjdk-17-slim AS build
COPY . .
RUN mvn clean package -DskipTests

# 2. 実行環境 (より汎用的なEclipse Temurinに変更)
FROM eclipse-temurin:17-jdk
COPY --from=build /target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java","-jar","/app.jar"]
