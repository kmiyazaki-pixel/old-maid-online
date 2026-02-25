# --- ステージ1: ビルド ---
FROM maven:3.8.8-eclipse-temurin-17 AS build
WORKDIR /app
COPY . .
# 依存関係を含めてパッケージ化
RUN mvn clean package -DskipTests

# --- ステージ2: 実行 ---
FROM eclipse-temurin:17-jre
WORKDIR /app
# ビルドステージで作成されたJARの中身を直接展開して、クラスパスのズレをなくします
COPY --from=build /app/target/app.jar app.jar
RUN jar -xf app.jar

# ポート開放
EXPOSE 8080

# 実行コマンド：クラス名を直接指定し、カレントディレクトリを検索対象にします
ENTRYPOINT ["java", "-cp", ".:app.jar", "OldMaidServer"]
