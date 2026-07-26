# ============ Stage 1: Maven 编译 ============
FROM maven:3.9-eclipse-temurin-17 AS builder
WORKDIR /build
# 先复制 pom.xml 利用 Docker 缓存加速依赖下载
COPY pom.xml .
RUN mvn -B dependency:go-offline
# 复制源码并编译打包
COPY src ./src
RUN mvn -B -DskipTests package

# ============ Stage 2: JRE 运行 ============
FROM eclipse-temurin:17-jre
# 安装 ffmpeg（视频合成必需）
RUN apt-get update && apt-get install -y --no-install-recommends ffmpeg \
    && rm -rf /var/lib/apt/lists/*
WORKDIR /app
COPY --from=builder /build/target/comic-backend-0.0.1-SNAPSHOT.jar app.jar
# 存储目录
RUN mkdir -p /data/comic-storage
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
