import requests, urllib3, json
urllib3.disable_warnings()

# Hardcoded test: use portal EnviewSessionId + credentials
user_id = "201918023"
user_pw = "D@lstn!0722"

sess = requests.Session()
sess.headers.update({
    "User-Agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36",
    "Accept-Language": "ko,en-US;q=0.9",
})

# Quick portal login to get EnviewSessionId
print("[1] Portal login...")
sess.get("https://portal.jvision.ac.kr/user/loginAuth.face",
         params={"langKnd":"ko","userId":user_id,"password":user_pw}, verify=False, timeout=10)
sess.post("https://portal.jvision.ac.kr/user/loginProcess.face",
          data={"langKnd":"ko","userId":user_id,"username":user_id,"password":user_pw},
          verify=False, timeout=10)

portal_sid = sess.cookies.get('JSESSIONID', domain='portal.jvision.ac.kr')
print(f"  portal JSESSIONID: {portal_sid[:20] if portal_sid else 'NONE'}...")

# Seed check.jvision.ac.kr with portal EnviewSessionId
enview_sid = sess.cookies.get('EnviewSessionId', domain='portal.jvision.ac.kr')
if enview_sid:
    sess.cookies.set('EnviewSessionId', enview_sid, domain='check.jvision.ac.kr', path='/')
    sess.cookies.set('EnviewLangKnd', 'ko', domain='check.jvision.ac.kr', path='/')
    print(f"  EnviewSessionId seeded: {enview_sid[:20]}...")
else:
    print("  No EnviewSessionId found!")

# Attend login
print("[2] Attend login...")
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

print(f"  Status: {resp.status_code}")
print(f"  Response: {resp.text[:300]}")
attend_jid = sess.cookies.get('JSESSIONID', domain='check.jvision.ac.kr')
print(f"  Attend JSESSIONID: {attend_jid}")
print(f"  All cookies:")
for c in sess.cookies:
    print(f"    [{c.domain}] {c.name}={c.value[:30]}...")
