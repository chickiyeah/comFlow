#!/usr/bin/env bash
# CampusFlow 개발 서버 배포 스크립트
# 사용: ./deploy.sh [all|backend|frontend]
# 기본값: all

set -e

SERVER="rndp@10.8.0.2"
SERVER_PW="cjm@0124"
REMOTE_DIR="/home/ruddls030/campusflow"
PLINK="/c/Program Files/PuTTY/plink"
PSCP="/c/Program Files/PuTTY/pscp"

MODE="${1:-all}"
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"

echo ">>> CampusFlow 배포 시작 (mode: $MODE)"

# ── 헬퍼: 원격 명령 실행
remote() {
  "$PLINK" -ssh -batch -pw "$SERVER_PW" "$SERVER" "$1" 2>&1
}

# ── 헬퍼: 단일 파일 업로드
upload_file() {
  "$PSCP" -pw "$SERVER_PW" -q "$1" "${SERVER}:${REMOTE_DIR}/$2"
}

# ── 헬퍼: src 디렉토리 전체 업로드 (tar 사용)
upload_src() {
  echo "  소스 패키징 중..."
  tar --exclude=./target \
      --exclude=./frontend/node_modules \
      --exclude=./.git \
      --exclude=./frontend/dist \
      -czf /tmp/campusflow_deploy.tar.gz . \
      -C "$SCRIPT_DIR" .
  echo "  업로드 중..."
  "$PSCP" -pw "$SERVER_PW" -q /tmp/campusflow_deploy.tar.gz "${SERVER}:/tmp/campusflow_deploy.tar.gz"
  remote "cd $REMOTE_DIR && tar xzf /tmp/campusflow_deploy.tar.gz && rm /tmp/campusflow_deploy.tar.gz"
  echo "  소스 전송 완료"
}

# ── Backend 배포
deploy_backend() {
  echo ">>> 백엔드 배포"
  # src 디렉토리만 전송
  echo "  백엔드 소스 업로드 중..."
  tar -czf /tmp/cf_backend.tar.gz -C "$SCRIPT_DIR" src pom.xml .env 2>/dev/null || \
  tar -czf /tmp/cf_backend.tar.gz -C "$SCRIPT_DIR" src pom.xml
  "$PSCP" -pw "$SERVER_PW" -q /tmp/cf_backend.tar.gz "${SERVER}:/tmp/cf_backend.tar.gz"
  remote "cd $REMOTE_DIR && tar xzf /tmp/cf_backend.tar.gz && rm /tmp/cf_backend.tar.gz"
  echo "  Docker 이미지 빌드 중... (약 30초)"
  remote "cd $REMOTE_DIR && docker-compose build backend 2>&1 | tail -5"
  remote "cd $REMOTE_DIR && docker-compose up -d backend"
  echo "  백엔드 재시작 완료"
}

# ── Frontend 배포
deploy_frontend() {
  echo ">>> 프론트엔드 배포"
  echo "  소스 업로드 중..."
  tar -czf /tmp/cf_frontend.tar.gz -C "$SCRIPT_DIR/frontend" src public package.json package-lock.json vite.config.js index.html 2>/dev/null || \
  tar -czf /tmp/cf_frontend.tar.gz -C "$SCRIPT_DIR/frontend" src package.json package-lock.json vite.config.js
  "$PSCP" -pw "$SERVER_PW" -q /tmp/cf_frontend.tar.gz "${SERVER}:/tmp/cf_frontend.tar.gz"
  remote "cd $REMOTE_DIR/frontend && tar xzf /tmp/cf_frontend.tar.gz && rm /tmp/cf_frontend.tar.gz"
  echo "  Docker 이미지 빌드 중..."
  remote "cd $REMOTE_DIR && docker-compose build frontend 2>&1 | tail -5"
  remote "cd $REMOTE_DIR && docker-compose up -d frontend"
  echo "  프론트엔드 재시작 완료"
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
echo "  백엔드: http://10.8.0.2:8080"
echo "  프론트: http://10.8.0.2:3000"
echo ""
remote "cd $REMOTE_DIR && docker-compose ps"
