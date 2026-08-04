#!/usr/bin/env bash
# CampusFlow 데모 데이터 시딩 (계정 201918023 / 나경원)
set -u
BASE="http://10.8.0.29:8080/api"
TOKEN=$(cat /tmp/cf_token.txt)
AUTH="Authorization: Bearer $TOKEN"
CT="Content-Type: application/json"
TMP=$(mktemp -d)
ok=0; fail=0

post() { # $1=path  $2=json
  local path="$1"; local json="$2"; local f="$TMP/p.json"
  printf '%s' "$json" > "$f"
  local code=$(curl -s -o "$TMP/r.json" -w "%{http_code}" --max-time 30 -X POST "$BASE/$path" -H "$AUTH" -H "$CT" -d @"$f")
  if [ "$code" = "200" ] || [ "$code" = "201" ]; then ok=$((ok+1)); echo "  OK  $path"; else fail=$((fail+1)); echo "  ERR($code) $path :: $(head -c 160 "$TMP/r.json")"; fi
}

echo "=== 시간표 (2026-1) ==="
post schedule '{"subjectName":"객체지향프로그래밍","subjectCode":"CS201","professor":"김도훈","room":"IT-301","dayOfWeek":"MONDAY","startTime":"09:00","endTime":"10:30","year":2026,"semester":1}'
post schedule '{"subjectName":"데이터베이스","subjectCode":"CS202","professor":"이수민","room":"IT-302","dayOfWeek":"MONDAY","startTime":"11:00","endTime":"12:30","year":2026,"semester":1}'
post schedule '{"subjectName":"알고리즘","subjectCode":"CS203","professor":"박정우","room":"IT-303","dayOfWeek":"TUESDAY","startTime":"09:00","endTime":"10:30","year":2026,"semester":1}'
post schedule '{"subjectName":"AI프롬프트엔지니어링","subjectCode":"AI201","professor":"최유진","room":"AI-201","dayOfWeek":"WEDNESDAY","startTime":"10:00","endTime":"11:30","year":2026,"semester":1}'
post schedule '{"subjectName":"해킹및침해대응","subjectCode":"SEC101","professor":"정민호","room":"SEC-101","dayOfWeek":"WEDNESDAY","startTime":"13:00","endTime":"14:30","year":2026,"semester":1}'
post schedule '{"subjectName":"웹프로그래밍","subjectCode":"CS204","professor":"한지원","room":"IT-304","dayOfWeek":"THURSDAY","startTime":"09:00","endTime":"10:30","year":2026,"semester":1}'
post schedule '{"subjectName":"캡스톤디자인","subjectCode":"CS210","professor":"김도훈","room":"IT-305","dayOfWeek":"FRIDAY","startTime":"13:00","endTime":"15:30","year":2026,"semester":1}'

echo "=== 수상내역 ==="
post awards '{"title":"교내 프로그래밍 경진대회 금상","organization":"전주비전대학교 컴퓨터정보과","level":"GOLD","awardDate":"2025-11-20","description":"알고리즘 문제해결 부문 1위"}'
post awards '{"title":"전북 IT 해커톤 우수상","organization":"전북IT협회","level":"SILVER","awardDate":"2025-09-15","description":"AI 학사관리 서비스로 우수상 수상"}'
post awards '{"title":"캡스톤 디자인 경진대회 장려상","organization":"컴퓨터정보과","level":"ENCOURAGEMENT","awardDate":"2026-05-30","description":"CampusFlow 프로젝트"}'

echo "=== 취업 준비 활동 ==="
post career/activities '{"type":"CERTIFICATE","status":"COMPLETED","title":"정보처리기사","organization":"한국산업인력공단","completedDate":"2025-08-20","score":"합격","memo":"필기/실기 1회 합격"}'
post career/activities '{"type":"CERTIFICATE","status":"IN_PROGRESS","title":"SQLD","organization":"한국데이터산업진흥원","targetDate":"2026-09-06","memo":"기출 2회독 중"}'
post career/activities '{"type":"LANGUAGE_TEST","status":"COMPLETED","title":"TOEIC","organization":"ETS","completedDate":"2025-12-01","score":"850점"}'
post career/activities '{"type":"TRAINING","status":"COMPLETED","title":"AWS 클라우드 부트캠프","organization":"멋쟁이사자처럼","completedDate":"2026-02-15","score":"수료"}'
post career/activities '{"type":"INTERNSHIP","status":"PLANNING","title":"백엔드 개발 인턴","organization":"네이버 클라우드","targetDate":"2026-12-01","memo":"하반기 지원 예정"}'

echo "=== 이력서 ==="
post resumes '{"title":"백엔드 개발자 이력서","summary":"Spring Boot 기반 백엔드와 AI 연동에 강점이 있는 컴퓨터정보과 2학년입니다. 캡스톤으로 학사관리 플랫폼을 풀스택 구현했습니다.","skills":"Java, Spring Boot, JPA, MySQL, React, AWS, Git","targetJob":"백엔드 개발자","portfolioIds":[2]}'

echo "=== 자기소개서 ==="
post cover-letters '{"title":"네이버 클라우드 백엔드 지원","companyName":"네이버 클라우드","jobTitle":"백엔드 개발자","content":"[지원동기] 대규모 트래픽을 다루는 클라우드 인프라에 매력을 느껴 지원했습니다.\n[성장과정] 캡스톤 프로젝트에서 Spring Boot로 50여 개의 REST API를 설계하며 백엔드 역량을 키웠습니다.\n[직무역량] JWT 인증, JPA 최적화, 외부 API 연동, AI 폴백 체인 설계 경험이 있습니다.\n[입사 후 포부] 안정적이고 확장 가능한 백엔드 시스템을 만드는 개발자가 되겠습니다."}'

echo "=== 스크랩한 채용공고 ==="
post career/saved-jobs '{"title":"BKR IT&Digital본부 Retail Solution팀 인턴사원 채용","company":"비케이알","location":"서울","url":"https://www.jobkorea.co.kr/Recruit/GI_Read/51123298","deadline":"2026-07-15","jobType":"인턴","source":"잡코리아"}'
post career/saved-jobs '{"title":"KB데이타시스템 IT분야 경력직원 모집","company":"KB데이타시스템","location":"서울","jobType":"정규직","source":"잡코리아"}'
post career/saved-jobs '{"title":"백엔드 개발자(Spring) 신입 채용","company":"카카오엔터프라이즈","location":"경기","jobType":"정규직","source":"잡코리아"}'

echo "=== 강의 리뷰 ==="
post reviews '{"subjectName":"데이터베이스","professor":"이수민","year":2026,"semester":1,"rating":5,"content":"실습 위주 수업이라 SQL 실력이 확실히 늘었어요.","anonymous":false}'
post reviews '{"subjectName":"객체지향프로그래밍","professor":"김도훈","year":2026,"semester":1,"rating":4,"content":"과제가 많지만 그만큼 실력이 오릅니다.","anonymous":true}'
post reviews '{"subjectName":"AI프롬프트엔지니어링","professor":"최유진","year":2026,"semester":1,"rating":5,"content":"최신 AI 트렌드를 실습으로 배울 수 있어 유익했습니다.","anonymous":false}'

echo ""
echo "=== 시딩 결과: 성공 $ok / 실패 $fail ==="
rm -rf "$TMP"
