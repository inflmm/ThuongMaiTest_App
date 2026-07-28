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
FROM eclipse-temurin:17-jdk

# Cài đặt thư viện libwebp & cwebp công cụ nén ảnh trên nền Linux (Debian/Ubuntu)
RUN apt-get update && apt-get install -y \
    webp \
    libwebp-dev \
    && rm -rf /var/lib/apt/lists/*

WORKDIR /app
COPY --from=builder /app/target/*.jar app.jar

# Nhận cổng động từ môi trường Render/Railway (mặc định 8080)
EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]