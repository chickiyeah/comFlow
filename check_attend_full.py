import requests, urllib3, json
urllib3.disable_warnings()

user_id = "201918023"
user_pw = "D@lstn!0722"

sess = requests.Session()
sess.headers.update({"User-Agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36"})

sess.get("https://portal.jvision.ac.kr/user/loginAuth.face",
         params={"langKnd":"ko","userId":user_id,"password":user_pw}, verify=False, timeout=10)
sess.post("https://portal.jvision.ac.kr/user/loginProcess.face",
          data={"langKnd":"ko","userId":user_id,"username":user_id,"password":user_pw},
          verify=False, timeout=10)

ikey_json = json.dumps({"duser_id": user_id, "duser_pw": user_pw})
resp = sess.post(
    "https://check.jvision.ac.kr/attend/iwin_sin",
    data={"ikey": ikey_json},
    headers={
        "Content-Type": "application/x-www-form-urlencoded; charset=UTF-8",
        "X-Requested-With": "XMLHttpRequest",
        "Referer": "https://check.jvision.ac.kr/jvision/student/",
        "Origin": "https://check.jvision.ac.kr",
    },
    verify=False, timeout=10, allow_redirects=True
)

print(f"Status: {resp.status_code}")
print(f"Content-Length: {len(resp.content)} bytes")
print(f"Content-Type: {resp.headers.get('Content-Type','')}")
print(f"Full response:")
try:
    data = resp.json()
    print(json.dumps(data, ensure_ascii=False, indent=2))
except:
    print(resp.text)
