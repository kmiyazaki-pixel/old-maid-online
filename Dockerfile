# --- ステージ1: ビルド ---
FROM maven:3.8.5-openjdk-17 AS build
COPY . .
# 依存関係を含めて1つの大きな実行ファイル(app.jar)を作る
RUN mvn clean package -DskipTests

# --- ステージ2: 実行 ---
FROM openjdk:17-jdk-slim
# ビルドステージで作られたJARファイルをコピー
COPY --from=build /target/app.jar app.jar
# 実行（これが「実行ボタン」の役割をします）
ENTRYPOINT ["java", "-jar", "/app.jar"]
