# CI 가 테스트를 통과한 jar 를 만든 뒤 이 이미지에 담는다(./gradlew build → build/libs/jobbell.jar).
# 이미지 안에서 다시 빌드하지 않는다. 테스트를 거치지 않은 jar 가 배포되는 경로를 없애기 위해서다.
FROM eclipse-temurin:21-jre

# 헬스체크용. 배포 스크립트와 compose 가 /actuator/health 를 이걸로 찌른다.
RUN apt-get update \
    && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/*

RUN useradd --system --uid 10001 --create-home jobbell \
    && mkdir -p /app/logs \
    && chown -R jobbell /app
USER jobbell
WORKDIR /app

COPY --chown=jobbell build/libs/jobbell.jar /app/jobbell.jar

ENV TZ=Asia/Seoul \
    SPRING_PROFILES_ACTIVE=prod \
    JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75 -XX:+ExitOnOutOfMemoryError"

EXPOSE 8080
HEALTHCHECK --interval=10s --timeout=3s --start-period=40s --retries=3 \
    CMD curl -fsS http://localhost:8080/actuator/health || exit 1

ENTRYPOINT ["java", "-jar", "/app/jobbell.jar"]
