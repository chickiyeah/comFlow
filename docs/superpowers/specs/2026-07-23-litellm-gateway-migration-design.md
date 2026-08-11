# CampusFlow AI → LiteLLM 게이트웨이 전체 이관 (설계)

- 작성일: 2026-07-23
- 상태: 승인됨 (사용자 확정)
- 근거 규칙: 노션 「AI 네이티브 인프라 — 라이브 현황」 — *모든 AI/LLM 사용은 LiteLLM 게이트웨이(`10.8.0.1:4000`) 한 곳만 경유. 클라이언트는 역할명만 요청, 자체 폴백체인 금지, 벤더키 대신 가상키.*

## 배경 / 문제

CampusFlow 백엔드의 AI 진입점 `AiFacadeService`가 jvision.ai / Ollama / Gemini를 **직접 호출**하고 내부에 3단 폴백을 하드코딩 → 게이트웨이 미경유 = 정책 위반(Langfuse 관측 사각). 게이트웨이 클라이언트 뼈대(`LiteLlmService`, `AiTextService`)는 이미 존재하나 `litellm.base-url` 미설정으로 **비활성** 상태였고, 그나마 NovaClass 포팅 기능만 이 경로를 탐.

## 목표

CampusFlow의 모든 AI **텍스트** 호출을 게이트웨이(role=`smart`, `smart→fast` 중앙 별칭) 단일 경로로 이관. 라우팅·폴백은 게이트웨이가 중앙 통제.

## 조달된 값 (게이트웨이 실측)

| 항목 | 값 |
|------|-----|
| 엔드포인트 | `http://10.8.0.1:4000` (OpenAI 호환, `/v1/chat/completions`, Bearer) |
| 가상키 | alias `campusflow`, `models=[smart,fast,gemini,frontier-haiku]`, `max_budget=$10 / 30d` (신규 발급) |
| 역할명 | `smart` (config `model_group_alias: smart→fast` 확인) |
| 음성(whisper) | 게이트웨이 **미등록** — 음성 전사 경로는 이번 범위 제외 |

## 변경 설계

1. **`.env`** — `LITELLM_BASE_URL`, `LITELLM_API_KEY`(가상키), `LITELLM_TEXT_MODEL=smart` 추가.
2. **`application.properties`** — `litellm.text-model` 기본값 `gpt-oss-120b`→`smart`. 음성 회귀 방지용 `litellm.audio-enabled=${LITELLM_AUDIO_ENABLED:false}` 신설. 주석 갱신.
3. **`LiteLlmService`** — 텍스트 모델 기본값 `smart`. `audio-enabled` 필드 + `isAudioEnabled()` 추가(base-url 활성화와 음성 활성화 분리). 텍스트/음성 클라이언트 로직 자체는 유지.
4. **`AiFacadeService`** — 내부를 `LiteLlmService.ask()` 단일 위임으로 교체. `@Cacheable` 유지. 13개 소비자 인터페이스 불변(무수정). 벤더 3서비스 필드 제거하되 **파일은 유지**(unwired, 롤백 대비).
5. **`AiTextService`** — LiteLlm/AiFacade 이중경로가 둘 다 게이트웨이가 되어 무의미 → `AiFacadeService.ask()` 단일 위임으로 단순화(NovaClass 텍스트도 캐시 이점).
6. **`MultimodalService`** — 음성 게이트를 `liteLlmService.isEnabled()` → `isAudioEnabled()`로 변경. whisper 미등록 상태에서 base-url을 켜도 음성은 기존과 동일하게 graceful 안내 유지(하드 에러 회귀 방지).

## 유지되는 파일 (unwired)

`JvisionAiService`, `OllamaService`, `GeminiService` — 런타임 경로에서 제거되나 파일·빈은 남김. 게이트웨이 장애 시 임시 롤백 경로로 재배선 가능.

## 안전성 / 트레이드오프

- **단일점**: 게이트웨이 다운 시 CampusFlow AI 전면 중단. 단 게이트웨이 자체가 Ollama→jvision→Claude 내부 폴백 보유 → 정책이 명시적으로 요구하는 형태. 사용자 '전체 이관' 선택으로 수용.
- **예산캡**: $10/30d. smart→fast는 로컬 Ollama($0)라 실비용은 Claude/Gemini 폴백 시에만 발생.
- **캐싱**: `AiFacadeService.@Cacheable`은 `(systemPrompt,userMessage)` 키 유지 — 게이트웨이 호출도 동일 캐시 적용.

## 검증

- 게이트웨이 `smart` 역할 스모크: 신규 가상키로 "서울" 정상 응답 (실측 완료).
- `mvn compile -q` 성공.
- 백엔드 기동 후 로드맵/어시스턴트 1건 실호출로 end-to-end 확인(권장).

## 범위 밖

- 음성(whisper) 게이트웨이 등록 — 별도 티켓.
- 이미지/비전 경로 — 미등록 상태 유지.
- `ClaudeApiService`/`OpenAiService`(레거시, 폴백체인 제외됨) — 이번 변경 무관.
