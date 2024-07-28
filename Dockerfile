FROM maven:3.9.8-eclipse-temurin-21 AS build

WORKDIR /app

COPY pom.xml .
COPY src ./src

RUN mvn clean package -DskipTests

FROM eclipse-temurin:21.0.4_7-jre-alpine

WORKDIR /app

RUN mkdir -p /app/log

COPY --from=build /app/target/*.jar ./rss_bot.jar

# 配置容器启动命令
CMD ["sh", "-c", "nohup java -jar rss_bot.jar > /app/log/rss_bot.log 2>&1 & tail -f /app/log/rss_bot.log"]
