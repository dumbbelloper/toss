
plugins {
    java
    id("org.springframework.boot") version "4.1.0"
    id("io.spring.dependency-management") version "1.1.7"
}

group = "com"
version = "0.0.1-SNAPSHOT"
description = "toss"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(25)
    }
}

repositories {
    mavenCentral()
}

dependencies {
    // --- web (RestClient, @HttpExchange 클라이언트, SSE, 대시보드) ---
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-thymeleaf")

    // --- 보안 ---
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-oauth2-client")        // 웹 BFF: OIDC 로그인 + 세션
    implementation("org.springframework.boot:spring-boot-starter-oauth2-resource-server") // 모바일: Bearer JWT 검증
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("io.projectreactor:reactor-core") // SSE 시세 팬아웃용 Sinks/Flux

    // --- persistence (PostgreSQL + Spring Data JDBC + Flyway) ---
    implementation("org.springframework.boot:spring-boot-starter-data-jdbc")
    // Boot 4 는 통합별 auto-config 가 별도 모듈 → Flyway auto-config 활성화에 필요
    implementation("org.springframework.boot:spring-boot-flyway")
    implementation("org.flywaydb:flyway-core")
    implementation("org.flywaydb:flyway-database-postgresql")
    runtimeOnly("org.postgresql:postgresql")

    // --- dev / lombok ---
    compileOnly("org.projectlombok:lombok")
    developmentOnly("org.springframework.boot:spring-boot-devtools")
    // spring-boot-docker-compose 는 사용하지 않는다(IDE working dir 의존성 문제).
    // 로컬 postgres 는 `docker compose up -d` 로 직접 띄우고, 접속 정보는 application.yml 에 명시.
    annotationProcessor("org.projectlombok:lombok")
    annotationProcessor("org.springframework.boot:spring-boot-configuration-processor")

    // --- test ---
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.security:spring-security-test")
    testImplementation("org.springframework.boot:spring-boot-testcontainers")
    testImplementation("org.testcontainers:testcontainers-junit-jupiter")
    testImplementation("org.testcontainers:testcontainers-postgresql")
    testCompileOnly("org.projectlombok:lombok")
    testAnnotationProcessor("org.projectlombok:lombok")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.withType<Test> {
    useJUnitPlatform()
}

// 로컬 dev: 백엔드(BFF)가 Keycloak(https://localhost:8443, mkcert 로컬 CA)을 호출할 때
// PKIX 검증을 통과하도록 dev 트러스트스토어를 JVM 에 주입한다. 파일이 있을 때만 적용.
// 생성: ../infra/keycloak/trust-jvm.sh  (기본 CA 번들 + mkcert rootCA → Toss API HTTPS 도 유지)
tasks.named<JavaExec>("bootRun") {
    val devTrust = file("../infra/keycloak/tls/dev-truststore.p12")
    if (devTrust.exists()) {
        jvmArgs(
            "-Djavax.net.ssl.trustStore=${devTrust.absolutePath}",
            "-Djavax.net.ssl.trustStorePassword=changeit",
        )
    }
}
