import urllib.request, json

# 서버(10.8.0.29)에서 인트라넷(10.8.0.14:8000) 직접 호출 테스트
body = json.dumps({"user_id": "201918023", "user_pw": "D@lstn!0722"}).encode()
req = urllib.request.Request(
    "http://10.8.0.14:8000/api/sync-profile",
    data=body,
    headers={"Content-Type": "application/json"},
    method="POST"
)
try:
    with urllib.request.urlopen(req, timeout=60) as r:
        print("HTTP:", r.status)
        d = json.loads(r.read())
        print("success:", d.get("success"))
        if d.get("data"):
            print("name:", d["data"].get("name"))
            print("studentId:", d["data"].get("studentId"))
except urllib.error.HTTPError as e:
    print("HTTPError:", e.code, e.reason)
    print(e.read().decode()[:500])
except Exception as e:
    print("Error:", type(e).__name__, e)
