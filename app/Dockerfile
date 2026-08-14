# ==========================================
# STAGE 1: Build file JAR bằng Maven
# ==========================================
FROM maven:3.9.6-eclipse-temurin-17 AS builder
WORKDIR /app
COPY . .
RUN mvn clean package -DskipTests

# ==========================================
# STAGE 2: Chạy ứng dụng & Cài đặt libwebp
# ==========================================
FROM eclipse-temurin:17-jre

WORKDIR /app
COPY --from=builder /app/target/*.jar app.jar

# Nhận cổng động từ môi trường Render/Railway (mặc định 8080)
EXPOSE 8080

# Giới hạn bộ nhớ heap của JVM để tránh sử dụng quá nhiều bộ nhớ trên các nền tảng đám mây
ENTRYPOINT ["java", \
  "-Xms64m", "-Xmx220m", \
  "-XX:MaxMetaspaceSize=96m", \
  "-XX:MaxDirectMemorySize=32m", \
  "-XX:ReservedCodeCacheSize=48m", \
  "-Xss256k", \
  "-XX:+UseSerialGC", \
  "-jar", "app.jar"]