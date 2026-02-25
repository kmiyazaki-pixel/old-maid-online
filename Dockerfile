# 1. サーバーを作る道具を準備
FROM maven:3.8.8-eclipse-temurin-17 AS build
COPY . .
RUN mvn clean package -DskipTests

# 2. サーバーを動かす
FROM eclipse-temurin:17-jre
COPY --from=build /target/*.jar app.jar
EXPOSE 8080
# 直接ファイルを指定して「これ動かして！」と叫ぶ設定
ENTRYPOINT ["java", "-cp", "app.jar", "OldMaidServer"]
