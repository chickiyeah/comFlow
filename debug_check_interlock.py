import requests, json, urllib3
urllib3.disable_warnings()

uid = "201918023"
upw = "D@lstn!0722"
CHK = "https://check.jvision.ac.kr"

sess = requests.Session()
sess.headers.update({"User-Agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36"})
ikey = json.dumps({"duser_id": uid, "duser_pw": upw})
r = sess.post(f"{CHK}/attend/iwin_sin",
    data={"ikey": ikey},
    headers={"Content-Type": "application/x-www-form-urlencoded; charset=UTF-8",
             "X-Requested-With": "XMLHttpRequest",
             "Referer": f"{CHK}/jvision/student/", "Origin": CHK},
    verify=False, timeout=20, allow_redirects=True)

print("HTTP:", r.status_code, "size:", len(r.content))
data = r.json()
rb = data.get("rollbook", [])
print(f"rollbook count: {len(rb)}")
if rb:
    print("keys:", list(rb[0].keys()))
    print("---")
    for c in rb:
        il = c.get("is_interlock", "MISSING")
        sc = c.get("sugang_code", c.get("sugang_cd", "?"))
        nm = c.get("sugang_name", c.get("sugang_nm", "?"))
        print(f"  is_interlock={il}  code={sc}  name={nm}")
else:
    print("rollbook empty, top-level keys:", list(data.keys()))
    print("raw:", r.text[:2000])
