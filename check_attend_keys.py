import requests, urllib3, json
urllib3.disable_warnings()

sess = requests.Session()
sess.headers.update({"User-Agent": "Mozilla/5.0"})
sess.get("https://portal.jvision.ac.kr/user/loginAuth.face",
         params={"langKnd":"ko","userId":"201918023","password":"D@lstn!0722"}, verify=False, timeout=10)
sess.post("https://portal.jvision.ac.kr/user/loginProcess.face",
          data={"langKnd":"ko","userId":"201918023","username":"201918023","password":"D@lstn!0722"},
          verify=False, timeout=10)

resp = sess.post("https://check.jvision.ac.kr/attend/iwin_sin",
    data={"ikey": json.dumps({"duser_id":"201918023","duser_pw":"D@lstn!0722"})},
    headers={"Content-Type":"application/x-www-form-urlencoded; charset=UTF-8",
             "X-Requested-With":"XMLHttpRequest","Referer":"https://check.jvision.ac.kr/jvision/student/","Origin":"https://check.jvision.ac.kr"},
    verify=False, timeout=10, allow_redirects=True)

data = resp.json()
print(f"Top-level keys: {list(data.keys())}")
for k, v in data.items():
    if isinstance(v, dict):
        print(f"  {k}: dict with keys {list(v.keys())[:10]}")
    elif isinstance(v, list):
        print(f"  {k}: list of {len(v)} items")
        if v:
            print(f"    first item keys: {list(v[0].keys()) if isinstance(v[0], dict) else type(v[0])}")
    else:
        print(f"  {k}: {str(v)[:100]}")
