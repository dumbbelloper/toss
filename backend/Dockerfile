# gotgan backend — Spring Boot 4.1 / Java 25. 빌드 컨텍스트 = backend/
# 멀티스테이지: gradle 래퍼로 bootJar → JRE 25 런타임
FROM eclipse-temurin:25-jdk AS build
WORKDIR /src
# 의존성 캐시 레이어 (래퍼/빌드스크립트 먼저)
COPY gradlew settings.gradle.kts build.gradle.kts ./
COPY gradle ./gradle
RUN chmod +x gradlew && ./gradlew --no-daemon --version
# 소스 빌드 (테스트는 CI 별도 단계에서)
COPY src ./src
RUN ./gradlew --no-daemon clean bootJar -x test

FROM eclipse-temurin:25-jre
WORKDIR /app
# Spring Boot 실행 가능 jar (toss-*.jar — gradle artifact 명)
COPY --from=build /src/build/libs/*-SNAPSHOT.jar app.jar
EXPOSE 8080
ENV JAVA_OPTS=""
ENTRYPOINT ["sh","-c","java $JAVA_OPTS -jar app.jar"]
