#!/usr/bin/env python3
"""
chroma-inspect : ChromaDB 벡터 인스펙션 CLI (읽기 전용)

쿼리를 넣었을 때
  1) 실제 임베딩 벡터가 어떻게 생겼는지 (차원 / norm / 앞부분 값)
  2) 그 벡터로 어떤 청크들이 몇 점(distance)에 걸리는지
  3) (선택) 청크 벡터들과 쿼리 벡터를 2D로 뿌려서 어느 클러스터로 갔는지
를 눈으로 보기 위한 도구.

핵심 전제
--------
컬렉션에 데이터를 넣을 때 쓴 임베딩 모델/경로와 *같은 것*으로 쿼리해야 한다.
다르면 벡터 공간이 어긋나서 검색 결과가 무의미해진다.
먼저 `collections` 로 메타데이터/차원을 확인하고, --model(또는 --embed-backend gateway
+ --gateway-model) 을 원본과 맞춰라.

임베딩 백엔드
-----------
  --embed-backend st       (기본) sentence-transformers 로컬 임베딩. --model 로 모델 지정.
  --embed-backend gateway  LiteLLM(OpenAI 호환) 게이트웨이 /v1/embeddings 호출.
                           게이트웨이로 적재한 컬렉션(예: nomic-embed-text)을 원본과
                           동일한 벡터로 조회할 때 사용.

가드레일
-------
이 도구는 **읽기 전용**이다. list_collections / get / peek / query / count 만 사용하며,
컬렉션에 add/update/delete 를 절대 하지 않는다. ChromaDB 볼륨도 건드리지 않는다.

설치
----
    pip install chromadb sentence-transformers numpy scikit-learn matplotlib
    # 클라이언트만 필요하면(게이트웨이 백엔드): pip install chromadb-client numpy
    # 2D를 UMAP으로 보고 싶으면: pip install umap-learn

사용 예시
--------
    # 1) 컬렉션 목록 + 메타데이터 + 저장 차원 확인 (무엇으로 임베딩됐는지 단서)
    python chroma_inspect.py collections --host 10.8.0.17 --port 8001

    # 2) 학과 RAG(komjeong) — sentence-transformers 원본과 동일
    python chroma_inspect.py query "졸업 요건" -c komjeong \
        --model sentence-transformers/paraphrase-multilingual-MiniLM-L12-v2 \
        --host 10.8.0.17 --port 8001

    # 3) 시맨틱 캐시(campusflow_qa_cache) — 게이트웨이 임베딩과 동일
    python chroma_inspect.py query "학사 일정 언제야" -c campusflow_qa_cache \
        --embed-backend gateway --gateway-model embed \
        --host 10.8.0.17 --port 8001            # 키는 LITELLM_KEY 환경변수 또는 --gateway-key
"""

import argparse
import os
import sys

import numpy as np


# ---------------------------------------------------------------------------
# 연결
# ---------------------------------------------------------------------------
def get_client(args):
    import chromadb

    if args.path:  # 로컬 임베디드(파일) 모드
        return chromadb.PersistentClient(path=args.path)
    # 서버(클라이언트-서버) 모드
    return chromadb.HttpClient(host=args.host, port=args.port)


# ---------------------------------------------------------------------------
# 임베딩 백엔드
# ---------------------------------------------------------------------------
_MODEL_CACHE = {}


def get_model(name):
    """sentence-transformers 모델을 로드(GPU 있으면 자동으로 CUDA 사용)."""
    if name in _MODEL_CACHE:
        return _MODEL_CACHE[name]
    from sentence_transformers import SentenceTransformer

    print(f"[*] 임베딩 모델 로딩: {name} ...", file=sys.stderr)
    model = SentenceTransformer(name)  # device 자동 선택
    _MODEL_CACHE[name] = model
    return model


def embed_text_st(name, text):
    """sentence-transformers 로 텍스트 하나를 임베딩해서 numpy 벡터로 반환."""
    model = get_model(name)
    vec = model.encode([text], normalize_embeddings=False)[0]
    return np.asarray(vec, dtype=np.float32)


def embed_text_gateway(args, text):
    """LiteLLM(OpenAI 호환) 게이트웨이 /v1/embeddings 로 임베딩. numpy 벡터 반환."""
    import json
    import urllib.request

    if not args.gateway_model:
        sys.exit("오류: --embed-backend gateway 에는 --gateway-model 이 필요합니다 "
                 "(게이트웨이에 등록된 임베딩 역할/모델명, 예: embed).")

    # nomic 계열은 task 프리픽스에 민감. 적재 시와 동일해야 검색이 의미 있다.
    payload = ("search_query: " + text) if args.nomic_prefix else text

    url = args.gateway_url.rstrip("/") + "/v1/embeddings"
    body = json.dumps({"model": args.gateway_model, "input": [payload]}).encode("utf-8")
    req = urllib.request.Request(url, data=body, method="POST")
    req.add_header("Content-Type", "application/json")
    key = args.gateway_key or os.environ.get("LITELLM_KEY")
    if key:
        req.add_header("Authorization", "Bearer " + key)

    try:
        with urllib.request.urlopen(req, timeout=args.gateway_timeout) as r:
            data = json.loads(r.read().decode("utf-8"))
    except Exception as e:
        detail = ""
        if hasattr(e, "read"):
            try:
                detail = " | " + e.read().decode("utf-8", "replace")[:300]
            except Exception:
                pass
        sys.exit(f"게이트웨이 임베딩 호출 실패: {e}{detail}")

    try:
        vec = data["data"][0]["embedding"]
    except (KeyError, IndexError, TypeError):
        sys.exit(f"게이트웨이 응답 형식 예상과 다름: {str(data)[:300]}")
    return np.asarray(vec, dtype=np.float32)


def embed_text(args, text):
    """백엔드에 따라 분기해 텍스트를 임베딩."""
    if getattr(args, "embed_backend", "st") == "gateway":
        return embed_text_gateway(args, text)
    return embed_text_st(args.model, text)


def model_label(args):
    """현재 백엔드/모델을 사람이 읽는 라벨로."""
    if getattr(args, "embed_backend", "st") == "gateway":
        pfx = " +search_query:" if args.nomic_prefix else ""
        return f"gateway:{args.gateway_model}{pfx}"
    return args.model


def stored_dim(col):
    """컬렉션에 저장된 벡터 차원을 peek 로 얻는다. 못 얻으면 None."""
    try:
        pk = col.peek(limit=1)
        embs = pk.get("embeddings")
        if embs is not None and len(embs) > 0:
            return len(embs[0])
    except Exception:
        pass
    return None


# ---------------------------------------------------------------------------
# 서브커맨드 (전부 읽기 전용)
# ---------------------------------------------------------------------------
def cmd_collections(args):
    """컬렉션 목록 + count + 메타데이터. 무엇으로 임베딩됐는지 단서를 찾는 용도."""
    client = get_client(args)
    cols = client.list_collections()
    if not cols:
        print("컬렉션 없음.")
        return
    for c in cols:
        col = client.get_collection(c.name)
        try:
            cnt = col.count()
        except Exception as e:
            cnt = f"(count 실패: {e})"
        print(f"\n■ {c.name}  |  항목 {cnt}개")
        print(f"    metadata: {c.metadata}")
        # 샘플 1개 peek — 저장된 벡터 차원을 보면 어떤 모델인지 좁힐 수 있다
        try:
            peek = col.peek(limit=1)
            embs = peek.get("embeddings")
            if embs is not None and len(embs) > 0:
                print(f"    벡터 차원: {len(embs[0])}  (384=MiniLM-L12, 768=nomic/일부, "
                      f"1024=BGE-M3/Qwen 계열, 1536/3072=OpenAI 등)")
            docs = peek.get("documents")
            if docs:
                sample = (docs[0] or "")[:80].replace("\n", " ")
                print(f"    문서 샘플: {sample}...")
        except Exception as e:
            print(f"    peek 실패: {e}")


def cmd_embed(args):
    """쿼리 → 벡터. 차원/norm/앞부분 값을 찍어 벡터가 어떻게 생겼는지 본다."""
    vec = embed_text(args, args.text)
    norm = float(np.linalg.norm(vec))
    print(f"입력       : {args.text!r}")
    print(f"임베딩     : {model_label(args)}")
    print(f"차원       : {vec.shape[0]}")
    print(f"L2 norm    : {norm:.4f}")
    print(f"min / max  : {vec.min():.4f} / {vec.max():.4f}")
    n = min(args.show, vec.shape[0])
    print(f"앞 {n}개 값 : {np.array2string(vec[:n], precision=4, separator=', ')}")


def cmd_query(args):
    """쿼리 벡터로 top-k 검색 → 어떤 청크가 몇 점에 걸리는지."""
    client = get_client(args)
    col = client.get_collection(args.collection)

    qvec = embed_text(args, args.text)

    # 차원 검증: 쿼리 벡터 차원 ≠ 컬렉션 저장 차원이면 검색 전에 중단 (무의미한 결과 방지)
    sd = stored_dim(col)
    if sd is not None and sd != qvec.shape[0]:
        print(f"⚠ 차원 불일치: 쿼리 {qvec.shape[0]} vs 저장 {sd}. "
              f"--model / --embed-backend 를 원본 임베딩과 맞춰라.", file=sys.stderr)
        sys.exit(2)

    res = col.query(
        query_embeddings=[qvec.tolist()],   # 텍스트가 아니라 '내가 만든 벡터'로 검색
        n_results=args.k,
        include=["documents", "distances", "metadatas"],
    )

    ids = res["ids"][0]
    docs = res["documents"][0]
    dists = res["distances"][0]
    metas = res["metadatas"][0]

    print(f"쿼리: {args.text!r}   (임베딩 {model_label(args)}, 차원 {qvec.shape[0]})")
    print(f"컬렉션: {args.collection}   top-{args.k}\n")
    print("distance 낮을수록 가까움 (기본 메트릭이 코사인/L2면 그렇다)\n")
    for i, (_id, d, doc, m) in enumerate(zip(ids, dists, docs, metas), 1):
        text = (doc or "").replace("\n", " ")
        if len(text) > args.width:
            text = text[: args.width] + "…"
        print(f"[{i}] dist={d:.4f}   id={_id}")
        print(f"    {text}")
        if m:
            print(f"    meta: {m}")
        print()


def cmd_plot(args):
    """청크 벡터 + 쿼리 벡터를 2D로 축소해 산점도 저장."""
    import matplotlib
    matplotlib.use("Agg")
    import matplotlib.pyplot as plt

    client = get_client(args)
    col = client.get_collection(args.collection)

    data = col.get(include=["embeddings", "documents"], limit=args.limit)
    embs = np.asarray(data["embeddings"], dtype=np.float32)
    if embs.size == 0:
        print("임베딩이 비어있음. include에 embeddings가 저장돼 있는지 확인.")
        return

    qvec = embed_text(args, args.text)
    if qvec.shape[0] != embs.shape[1]:
        print(f"⚠ 차원 불일치: 쿼리 {qvec.shape[0]} vs 저장 {embs.shape[1]}. "
              f"--model / --embed-backend 를 원본 임베딩 모델과 맞춰라.")
        return

    allv = np.vstack([embs, qvec[None, :]])

    if args.method == "umap":
        import umap
        reducer = umap.UMAP(n_components=2, metric="cosine", random_state=42)
    else:
        from sklearn.decomposition import PCA
        reducer = PCA(n_components=2)
    xy = reducer.fit_transform(allv)

    chunks_xy, q_xy = xy[:-1], xy[-1]
    plt.figure(figsize=(8, 6))
    plt.scatter(chunks_xy[:, 0], chunks_xy[:, 1], s=12, alpha=0.5, label="chunks")
    plt.scatter([q_xy[0]], [q_xy[1]], s=140, marker="*",
                color="red", label="query", zorder=5)
    plt.title(f"{args.method.upper()} · query={args.text!r}")
    plt.legend()
    plt.tight_layout()
    out = args.out or "query.png"
    plt.savefig(out, dpi=130)
    print(f"저장됨: {out}  (청크 {len(chunks_xy)}개 + 쿼리 1개)")


# ---------------------------------------------------------------------------
# argparse
# ---------------------------------------------------------------------------
def build_parser():
    p = argparse.ArgumentParser(description="ChromaDB 벡터 인스펙션 CLI (읽기 전용)")
    p.add_argument("--host", default="localhost", help="Chroma 서버 호스트")
    p.add_argument("--port", type=int, default=8000, help="Chroma 서버 포트")
    p.add_argument("--path", default=None,
                   help="로컬 파일 모드로 열 때의 경로 (지정 시 host/port 무시)")

    # 임베딩 백엔드
    p.add_argument("--embed-backend", choices=["st", "gateway"], default="st",
                   help="임베딩 생성 경로: st=sentence-transformers(기본), "
                        "gateway=LiteLLM /v1/embeddings")
    p.add_argument("--model", default="BAAI/bge-m3",
                   help="[st] sentence-transformers 임베딩 모델 (컬렉션 원본과 일치)")
    # 게이트웨이 백엔드 설정
    p.add_argument("--gateway-url", default="http://10.8.0.1:4000",
                   help="[gateway] LiteLLM 게이트웨이 베이스 URL")
    p.add_argument("--gateway-model", default=None,
                   help="[gateway] 게이트웨이에 등록된 임베딩 역할/모델명 (예: embed)")
    p.add_argument("--gateway-key", default=None,
                   help="[gateway] API 키. 미지정 시 환경변수 LITELLM_KEY 사용")
    p.add_argument("--gateway-timeout", type=float, default=30.0,
                   help="[gateway] 임베딩 호출 타임아웃(초)")
    p.add_argument("--nomic-prefix", action=argparse.BooleanOptionalAction, default=False,
                   help="[gateway] nomic 계열 쿼리에 'search_query: ' 프리픽스 적용 "
                        "(적재 시와 동일하게 맞춰야 함. 기본 off)")

    sub = p.add_subparsers(dest="cmd", required=True)

    sp = sub.add_parser("collections", help="컬렉션 목록/메타데이터/차원")
    sp.set_defaults(func=cmd_collections)

    sp = sub.add_parser("embed", help="쿼리를 벡터로 변환해 값 확인")
    sp.add_argument("text")
    sp.add_argument("--show", type=int, default=10, help="앞에서 보여줄 차원 수")
    sp.set_defaults(func=cmd_embed)

    sp = sub.add_parser("query", help="top-k 검색 결과 + distance (+ id)")
    sp.add_argument("text")
    sp.add_argument("-c", "--collection", required=True)
    sp.add_argument("-k", type=int, default=5)
    sp.add_argument("--width", type=int, default=200, help="문서 미리보기 글자수")
    sp.set_defaults(func=cmd_query)

    sp = sub.add_parser("plot", help="2D 산점도 저장")
    sp.add_argument("text")
    sp.add_argument("-c", "--collection", required=True)
    sp.add_argument("--method", choices=["pca", "umap"], default="pca")
    sp.add_argument("--limit", type=int, default=2000, help="가져올 청크 최대 수")
    sp.add_argument("--out", default=None, help="저장 파일명 (기본 query.png)")
    sp.set_defaults(func=cmd_plot)

    return p


def main():
    args = build_parser().parse_args()
    args.func(args)


if __name__ == "__main__":
    main()
