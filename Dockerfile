# 基础镜像（Java 8）
FROM openjdk:8-jre-slim

# 维护者信息
LABEL maintainer="3127103271@qq.com"

# 复制应用 JAR 包（假设 Maven 构建输出到 target/app-demo.jar）
COPY target/app-demo.jar /app/app-demo.jar

# 暴露应用端口（如 8080，需与 docker-compose 一致）
EXPOSE 8081

# 启动命令
ENTRYPOINT ["java", "-jar", "/app/app-demo.jar"]
