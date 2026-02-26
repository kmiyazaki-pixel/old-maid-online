FROM maven:3.8.8-eclipse-temurin-17 AS build
WORKDIR /app
COPY . .
RUN mvn clean package -DskipTests

FROM eclipse-temurin:17-jre
WORKDIR /app
# ビルドしたapp.jarをそのままコピー
COPY --from=build /app/target/app.jar app.jar
EXPOSE 8080
# jarを直接叩くのではなく、中身を全部指定して確実にOldMaidServerを探させます
ENTRYPOINT ["java", "-cp", "app.jar", "OldMaidServer"]
