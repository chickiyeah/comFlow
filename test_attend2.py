import requests, urllib3, json, time
urllib3.disable_warnings()

uid = "201918023"
upw = "D@lstn!0722"

# Quick portal login (no full sync - just SSO + cookies)
print("[1] Quick portal login...")
t0 = time.time()
sess = requests.Session()
sess.headers.update({"User-Agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36", "Accept-Language": "ko,en-US;q=0.9"})
sess.get("https://portal.jvision.ac.kr/user/loginAuth.face", params={"langKnd":"ko","userId":uid,"password":upw}, verify=False, timeout=10)
sess.post("https://portal.jvision.ac.kr/user/loginProcess.face", data={"langKnd":"ko","userId":uid,"username":uid,"password":upw}, verify=False, timeout=10)
portal_cookies = [{"name":c.name,"value":c.value,"domain":c.domain,"path":c.path} for c in sess.cookies]
print(f"  Done in {time.time()-t0:.1f}s, {len(portal_cookies)} cookies")

# Test attend/all with quick portal cookies
print("[2] attend/all (parallel)...")
t1 = time.time()
r = requests.post("http://10.8.0.14:8000/api/attend/all",
    json={"user_id":uid,"user_pw":upw,"session_cookies": portal_cookies},
    timeout=60)
print(f"  Done in {time.time()-t1:.1f}s status={r.status_code}")

data = r.json()
print(f"success={data.get('success')}")
if data.get('success'):
    print(f"학기: {data.get('semester')}")
    courses = data.get('courses', [])
    print(f"과목 수: {len(courses)}")
    for c in courses:
        attend_raw = c.get('attend_raw', {})
        top_keys = list(attend_raw.keys()) if isinstance(attend_raw, dict) else []
        is_lock = c.get('is_interlock', '?')
        print(f"  [interlock={is_lock}] {c.get('sugang_name','?')} → {top_keys}")
else:
    print(f"Error: {data.get('message','')}")
