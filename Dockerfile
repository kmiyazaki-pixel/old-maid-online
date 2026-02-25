# --- ステージ1: ビルド ---
FROM maven:3.8.8-eclipse-temurin-17 AS build
WORKDIR /app
COPY . .
# 依存関係を含めて、実行可能なJARファイルを生成
RUN mvn clean package -DskipTests

# --- ステージ2: 実行 ---
FROM eclipse-temurin:17-jre
WORKDIR /app
# ビルドステージで作成されたファイルをコピー
COPY --from=build /app/target/app.jar app.jar
# ポート開放の宣言（Render用）
EXPOSE 8080
# 実行コマンド：直接クラスを指定することで manifest エラーを回避します
ENTRYPOINT ["java", "-cp", "app.jar", "OldMaidServer"]
