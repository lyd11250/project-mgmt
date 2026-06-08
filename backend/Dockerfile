# ---- 构建阶段 ----
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
# 国内 Maven 镜像（阿里云），加速依赖下载
COPY settings.xml /root/.m2/settings.xml
COPY pom.xml .
RUN mvn -q -s /root/.m2/settings.xml dependency:go-offline
COPY src ./src
RUN mvn -q -s /root/.m2/settings.xml -DskipTests package

# ---- 运行阶段 ----
FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /app/target/collab-platform.jar app.jar
EXPOSE 8080
ENV SPRING_PROFILES_ACTIVE=prod
ENTRYPOINT ["java", "-jar", "app.jar"]
