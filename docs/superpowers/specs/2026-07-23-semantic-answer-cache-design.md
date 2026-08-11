# CampusFlow 시맨틱 답변 캐시 (설계)

- 작성일: 2026-07-23
- 상태: 승인됨 (사용자 확정)

## 목표
일반 Q&A(komjeong 챗·kmate 튜터)에서 **의미적으로 유사한 과거 질문**이 오면 AI를 호출하지 않고 저장된 답변을 즉시 반환한다. 단 사용자 프롬프트에 **최신성 키워드**(새로/최신/최신화 등)가 있으면 캐시를 우회하고 AI를 호출한다. 캐시는 **best-effort 최적화**로, 어떤 실패도 요청을 깨지 않는다.

## 커버리지 (승인)
- **일반 Q&A만**: `KomjeongChatController.chat`(랜딩 검색), `KmateService` 튜터.
- 개인화/구조화 답변(로드맵·자소서·포트폴리오)은 **제외**(오답 위험).

## 아키텍처 (앱 레벨, write-through)
신규 패키지 `domain/ai/cache`:
- **`GatewayEmbeddingClient`** — `float[] embed(String)`: 게이트웨이 `/v1/embeddings`(model=`embed`=nomic-embed-text, 768-dim), campusflow 가상키. **짧은 타임아웃**(기본 4s), 실패 시 `null`.
- **`ChromaCacheStore`** — ChromaDB v2(`10.8.0.17:8001`) 전용 컬렉션 `campusflow_qa_cache`(cosine). 기동 시 `get_or_create`로 collection id 확보.
  - `lookup(float[] emb) → Optional<Hit>`: `query`(n_results=3, include distances+metadatas). 클라이언트측에서 `거리 ≤ maxDistance` && `ts ≥ now−ttl` 인 최근접 선택.
  - `store(float[] emb, String q, String a)`: `add`(id=UUID, metadata={answer, ts}). 비동기(`CompletableFuture.runAsync`).
- **`SemanticCacheService`** — `String getOrCompute(String question, Supplier<String> aiCall)`.

### 요청 흐름
```
getOrCompute(q, aiCall):
  if !enabled: return aiCall.get()
  fresh = hasFreshnessKeyword(q)
  emb = embed(q)                         # best-effort, null on fail/timeout
  if emb != null && !fresh:
      hit = store.lookup(emb)
      if hit present: log HIT; return hit.answer      # ← AI 안 씀
  answer = aiCall.get()                   # miss OR fresh OR embed 실패
  if emb != null && answer 유효: asyncStore(emb, q, answer)   # write-through/갱신
  return answer
```
- embed 실패 → 순수 통과(캐시 무시, 오늘과 동일 동작).
- fresh → lookup 스킵하되 저장은 함(최신 답변으로 캐시 갱신).

## ChromaDB v2 API (실측 확정)
- base: `http://10.8.0.17:8001/api/v2/tenants/default_tenant/databases/default_database`
- create: `POST /collections` `{name, configuration:{hnsw:{space:"cosine"}}, get_or_create:true}` → 200 `{id}`
- add: `POST /collections/{id}/add` `{ids,embeddings:[[..]],documents,metadatas}` → 201
- query: `POST /collections/{id}/query` `{query_embeddings:[[..]],n_results,include:["distances","metadatas"]}` → 200 `{distances:[[..]],metadatas:[[..]]}`
- 거리 = cosine distance(1−sim). 동일방향≈0, 직교=1.

## 설정 (application.properties, 전부 튜닝 가능)
| 키 | 기본 |
|---|---|
| `semantic-cache.enabled` | true |
| `semantic-cache.similarity-threshold` | 0.93 (→ maxDistance 0.07) |
| `semantic-cache.ttl-days` | 30 |
| `semantic-cache.embed-timeout-ms` | 4000 |
| `semantic-cache.freshness-keywords` | 새로,새로운,최신,최신화,업데이트,요즘,올해,지금,현재,오늘,최근 |
| `chroma.base-url` | http://10.8.0.17:8001 |
| `chroma.collection` | campusflow_qa_cache |

## 안전장치
- **best-effort**: embed/chroma 예외는 전부 잡아 로그 후 통과. 캐시가 요청을 절대 실패시키지 않음.
- **보수적 임계값**: 0.93(오답 회피). 실데이터로 후속 캘리브레이션.
- **TTL**: 30일 초과 엔트리는 조회 시 무시(ts 메타 필터, 클라이언트측).
- write-through **비동기** → 응답 지연 0.
- 빈 답변은 저장 안 함.

## 통합
- `KomjeongChatController.chat`: 답변 계산을 `Supplier`로 감싸 `getOrCompute(question, supplier)`.
- `KmateService` 튜터 경로 동일.

## 검증
- 유닛: 최신성 키워드 감지, 임계값 경계(hit/miss), embed=null 통과, 빈답변 미저장.
- E2E(운영): 동일/유사 질문 2회 → 2번째 캐시 HIT(로그·저지연) / "새로" 접두 → AI 재호출.

## 범위 밖
- Langfuse 배치 백필(추후 cold-start 보강 시).
- 캐시 엔트리 프루닝 잡(현재 TTL 조회필터로 충분, 성장 시 별도).
- 개인화/구조화 답변 캐싱.
