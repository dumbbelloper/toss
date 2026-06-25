#!/usr/bin/env bash
#
# 백엔드(BFF)의 JVM 이 mkcert 로컬 CA 를 신뢰하도록 dev 트러스트스토어를 생성한다.
# Spring 이 Keycloak(https://localhost:8443)의 discovery/token/jwks 를 HTTPS 로 호출할 때
# PKIX 검증을 통과시키기 위함.
#
# 기반은 백엔드 툴체인 JDK(Corretto 25)의 기본 cacerts 다 → 실제 CA 신뢰를 유지하므로
# Toss Open API(실 인증서) HTTPS 호출도 그대로 동작한다. mkcert rootCA 만 추가한다.
#
# bootRun 이 이 파일을 -Djavax.net.ssl.trustStore 로 자동 주입한다(backend/build.gradle.kts).
# 재실행은 멱등(덮어씀). JDK 경로는 JAVA25_HOME 으로 덮어쓸 수 있다.
#
set -euo pipefail

HERE="$(cd "$(dirname "$0")" && pwd)"
TLS_DIR="$HERE/tls"
DEST="$TLS_DIR/dev-truststore.p12"
STOREPASS="changeit"   # JDK cacerts 기본 비밀번호 (로컬 dev 전용)

JDK="${JAVA25_HOME:-$HOME/.sdkman/candidates/java/25.0.3-amzn}"
SRC_CACERTS="$JDK/lib/security/cacerts"
ROOTCA="$(mkcert -CAROOT)/rootCA.pem"

[ -f "$SRC_CACERTS" ] || { echo "오류: JDK cacerts 없음: $SRC_CACERTS (JAVA25_HOME 지정 필요)"; exit 1; }
[ -f "$ROOTCA" ]      || { echo "오류: mkcert rootCA 없음: $ROOTCA (mkcert 설치/실행 필요)"; exit 1; }

mkdir -p "$TLS_DIR"
cp "$SRC_CACERTS" "$DEST"
keytool -importcert -noprompt -storepass "$STOREPASS" \
        -keystore "$DEST" -alias mkcert-rootca -file "$ROOTCA"

echo "✓ 생성: $DEST"
echo "  (기본 CA 번들 + mkcert rootCA 'mkcert-rootca')"
