import requests, urllib3, json, time
from concurrent.futures import ThreadPoolExecutor, as_completed
urllib3.disable_warnings()

uid = "201918023"
upw = "D@lstn!0722"
ATTEND = "https://check.jvision.ac.kr"

# Step 1: Portal login
t0 = time.time()
sess = requests.Session()
sess.headers.update({"User-Agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36"})
sess.get("https://portal.jvision.ac.kr/user/loginAuth.face", params={"langKnd":"ko","userId":uid,"password":upw}, verify=False, timeout=10)
r = sess.post("https://portal.jvision.ac.kr/user/loginProcess.face", data={"langKnd":"ko","userId":uid,"username":uid,"password":upw}, verify=False, timeout=10)
print(f"[1] Portal login: {time.time()-t0:.2f}s, status={r.status_code}")
print(f"    Cookies: {[(c.name,c.domain) for c in sess.cookies]}")

# Step 2: Attend login
t1 = time.time()
ikey = json.dumps({"duser_id":uid,"duser_pw":upw})
r2 = sess.post(f"{ATTEND}/attend/iwin_sin", data={"ikey":ikey},
    headers={"Content-Type":"application/x-www-form-urlencoded; charset=UTF-8","X-Requested-With":"XMLHttpRequest",
             "Referer":f"{ATTEND}/jvision/student/","Origin":ATTEND},
    verify=False, timeout=20, allow_redirects=True)
print(f"[2] Attend login: {time.time()-t1:.2f}s, status={r2.status_code}, size={len(r2.content)} bytes")
d2 = r2.json()
ok = d2.get("xidedu",{}).get("xmsg") == "Ok"
print(f"    Login success: {ok}")
rollbook = d2.get("rollbook",[])
print(f"    rollbook: {len(rollbook)} courses")

attend_jid = sess.cookies.get("JSESSIONID", domain="check.jvision.ac.kr")
print(f"    JSESSIONID: {attend_jid}")

# Step 3: One course attendance (time it)
if rollbook:
    t2 = time.time()
    c0 = rollbook[0]
    ikey2 = json.dumps({"dclass": c0["sugang_code"], "duser_id": uid})
    r3 = sess.post(f"{ATTEND}/attend/iwin_st_chulseokbu", data={"ikey":ikey2},
        headers={"Content-Type":"application/x-www-form-urlencoded; charset=UTF-8","X-Requested-With":"XMLHttpRequest",
                 "Referer":f"{ATTEND}/jvision/student/","Origin":ATTEND},
        verify=False, timeout=15)
    print(f"[3] 1st course attend: {time.time()-t2:.2f}s, status={r3.status_code}, size={len(r3.content)} bytes")
    d3 = r3.json()
    print(f"    keys: {list(d3.keys())}")

    # Step 4: Parallel all courses
    print(f"[4] Parallel all {len(rollbook)} courses...")
    t3 = time.time()

    def fetch_one(course):
        s2 = requests.Session()
        for c in sess.cookies:
            s2.cookies.set(c.name, c.value, domain=c.domain, path=c.path)
        ik = json.dumps({"dclass": course["sugang_code"], "duser_id": uid})
        try:
            rr = s2.post(f"{ATTEND}/attend/iwin_st_chulseokbu", data={"ikey":ik},
                headers={"Content-Type":"application/x-www-form-urlencoded; charset=UTF-8",
                         "X-Requested-With":"XMLHttpRequest","Referer":f"{ATTEND}/jvision/student/","Origin":ATTEND},
                verify=False, timeout=10)
            return course["sugang_name"], rr.status_code, len(rr.content)
        except Exception as e:
            return course["sugang_name"], "ERR", str(e)[:50]

    results = []
    with ThreadPoolExecutor(max_workers=8) as ex:
        futs = {ex.submit(fetch_one, c): c for c in rollbook if c.get("sugang_code")}
        for f in as_completed(futs):
            results.append(f.result())

    print(f"    All done in {time.time()-t3:.2f}s")
    for r in results:
        print(f"    {r[0]}: status={r[1]} size={r[2]}")
