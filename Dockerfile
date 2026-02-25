# 1. サーバーをビルド（お弁当を作る）
FROM maven:3.8.8-eclipse-temurin-17 AS build
WORKDIR /app
COPY . .
RUN mvn clean package -DskipTests

# 2. サーバーを起動（お弁当を食べる）
FROM eclipse-temurin:17-jre
WORKDIR /app
# ビルドされた「中身」だけを直接コピーする
COPY --from=build /app/target/classes/ .
# 必要なライブラリ(WebSocket)を読み込めるようにして実行！
COPY --from=build /app/target/app.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-cp", ".:app.jar", "OldMaidServer"]
