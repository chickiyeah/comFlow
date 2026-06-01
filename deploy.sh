#!/usr/bin/env bash
# CampusFlow Windows 서버(10.8.0.29) 배포 스크립트
# 사용: ./deploy.sh [all|backend|frontend]
# 기본값: all

set -e

SERVER="user@10.8.0.29"
SERVER_PW='D@lstn!0722'
PLINK="/c/Program Files/PuTTY/plink"
PSCP="/c/Program Files/PuTTY/pscp"
MVN="C:/apache-maven-3.9.15-bin/apache-maven-3.9.15/bin/mvn.cmd"

MODE="${1:-all}"
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"

echo ">>> CampusFlow 배포 시작 (mode: $MODE, target: 10.8.0.29)"

remote() {
  "$PLINK" -ssh -batch -pw "$SERVER_PW" "$SERVER" "$1" 2>&1
}

# ── Backend 배포 (JAR 빌드 후 전송 + NSSM 재시작)
deploy_backend() {
  echo ">>> 백엔드 배포"
  echo "  JAR 빌드 중..."
  "$MVN" -f "$SCRIPT_DIR/pom.xml" package -DskipTests -q
  JAR=$(ls "$SCRIPT_DIR/target/"*.jar 2>/dev/null | head -1)
  echo "  JAR 전송: $JAR"
  "$PSCP" -pw "$SERVER_PW" -q "$JAR" "${SERVER}:C:/campusflow/campusflow.jar"
  echo "  NSSM 서비스 재시작..."
  remote "nssm restart CampusFlowBackend"
  echo "  백엔드 재시작 완료"
}

# ── Frontend 배포 (빌드 후 dist 전송)
deploy_frontend() {
  echo ">>> 프론트엔드 배포"
  echo "  Vite 빌드 중..."
  (cd "$SCRIPT_DIR/frontend" && npm run build)
  echo "  dist 전송 중..."
  "$PSCP" -pw "$SERVER_PW" -r -q "$SCRIPT_DIR/frontend/dist/." "${SERVER}:C:/campusflow/frontend/"
  echo "  nginx 리로드..."
  remote "C:/nginx/nginx.exe -s reload"
  echo "  프론트엔드 배포 완료"
}

# ── 실행
case "$MODE" in
  backend)  deploy_backend ;;
  frontend) deploy_frontend ;;
  all)
    deploy_backend
    deploy_frontend
    ;;
  *)
    echo "사용법: $0 [all|backend|frontend]"
    exit 1
    ;;
esac

echo ""
echo "✓ 배포 완료!"
echo "  백엔드: http://10.8.0.29:8080"
echo "  프론트: http://10.8.0.29:3000"
echo ""
