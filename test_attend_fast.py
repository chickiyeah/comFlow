import requests, urllib3, json, time
urllib3.disable_warnings()

# 1. Get portal cookies via sync
print("[1] Portal sync...")
t0 = time.time()
sync = requests.post("http://10.8.0.14:8000/api/sync-profile",
    json={"user_id":"201918023","user_pw":"D@lstn!0722"}, timeout=90)
print(f"  Sync done in {time.time()-t0:.1f}s")
cookies = sync.json()["data"]["sessionCookies"]

# 2. Call attend/all with stored cookies
print("[2] attend/all...")
t1 = time.time()
r = requests.post("http://10.8.0.14:8000/api/attend/all",
    json={"user_id":"201918023","user_pw":"D@lstn!0722","session_cookies": cookies},
    timeout=60)
print(f"  Done in {time.time()-t1:.1f}s")

data = r.json()
print(f"success={data.get('success')}")
if data.get('success'):
    print(f"학기: {data.get('semester')}")
    courses = data.get('courses', [])
    print(f"과목 수: {len(courses)}")
    for c in courses:
        keys = list(c.get('attend_raw',{}).keys()) if isinstance(c.get('attend_raw'),dict) else []
        is_lock = c.get('is_interlock', 0)
        print(f"  [is_interlock={is_lock}] {c.get('sugang_name','?')} → attend_raw keys: {keys}")
else:
    print(f"Error: {data.get('message','')}")
