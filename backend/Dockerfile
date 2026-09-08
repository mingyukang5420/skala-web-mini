# ---------- 1단계: 빌드 ----------
FROM eclipse-temurin:21-jdk-alpine AS build
WORKDIR /app

# Gradle wrapper와 설정 파일만 먼저 복사해서 의존성 캐시 활용
COPY gradlew .
COPY gradle ./gradle
COPY build.gradle settings.gradle ./
RUN chmod +x gradlew && ./gradlew dependencies --no-daemon || true

COPY src ./src
RUN ./gradlew clean bootJar --no-daemon -x test

# ---------- 2단계: 실행 (경량 JRE 이미지) ----------
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

COPY --from=build /app/build/libs/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
