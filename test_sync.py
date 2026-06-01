import urllib.request, json

body = json.dumps({"user_id": "201918023", "user_pw": "D@lstn!0722"}).encode()
req = urllib.request.Request(
    "http://localhost:8000/api/sync-profile",
    data=body,
    headers={"Content-Type": "application/json"},
    method="POST"
)
try:
    with urllib.request.urlopen(req, timeout=60) as r:
        print("HTTP:", r.status)
        print(r.read().decode()[:500])
except urllib.error.HTTPError as e:
    print("HTTPError:", e.code, e.reason)
    print(e.read().decode()[:500])
except Exception as e:
    print("Error:", e)
