FROM maven:3.8.8-eclipse-temurin-17
WORKDIR /app
COPY . .

# 直接コンパイルして、クラスファイルとライブラリを同じ場所に置く
RUN mvn dependency:copy-dependencies
RUN javac -cp "target/dependency/*" OldMaidServer.java

EXPOSE 8080

# 「ここにファイルがあるから動かせ！」と直接指定
CMD ["java", "-cp", ".:target/dependency/*", "OldMaidServer"]
