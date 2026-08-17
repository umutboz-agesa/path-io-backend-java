# ── Build ────────────────────────────────────────────────────────────────────
FROM eclipse-temurin:21-jdk-alpine AS build
WORKDIR /workspace

# Wrapper ve build tanımları önce kopyalanır — bağımlılık katmanı cache'lenir.
COPY gradlew settings.gradle build.gradle ./
COPY gradle gradle
COPY appinsight-common appinsight-common
COPY appinsight-admin appinsight-admin

RUN ./gradlew :appinsight-admin:appinsight-admin-service:bootJar --no-daemon

# ── Runtime ──────────────────────────────────────────────────────────────────
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# OpenShift rastgele UID ile çalıştırır; root olmayan kullanıcı ve grup izni gerekir.
RUN addgroup -S appinsight && adduser -S appinsight -G appinsight
USER appinsight

COPY --from=build /workspace/appinsight-admin/appinsight-admin-service/build/libs/*.jar app.jar

EXPOSE 8080
ENV JAVA_OPTS=""

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/app.jar"]
