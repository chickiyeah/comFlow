import urllib.request, json, sys

body = json.dumps({
    "user_id": "201918023",
    "user_pw": "D@lstn!0722",
    "session_cookies": [],
    "year": "2026",
    "term": "1"
}).encode()

req = urllib.request.Request(
    "http://localhost:8000/api/attendance/combined",
    data=body,
    headers={"Content-Type": "application/json"},
    method="POST"
)
try:
    with urllib.request.urlopen(req, timeout=90) as resp:
        d = json.loads(resp.read())
    print("success:", d.get("success"))
    courses = d.get("courses", [])
    print("courses:", len(courses))
    for c in courses:
        src  = c.get("source", "?")
        name = c.get("course_name", c.get("sugang_name", "?"))
        p    = c.get("present", 0)
        ab   = c.get("absent", 0)
        nc   = c.get("not_checked", 0)
        err  = c.get("error", "")
        if err:
            print(f"  [{src}] ERROR: {err[:80]}")
        else:
            print(f"  [{src}] {name}  출석={p} 결석={ab} 미출결={nc}")
    print("summary:", d.get("summary"))
except Exception as e:
    print("ERROR:", e)
