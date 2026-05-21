import requests, urllib3, re
urllib3.disable_warnings()

uid = "201918023"
upw = "D@lstn!0722"
LMS = "https://lms.jvision.ac.kr"
KLASS = "A20261118031052182850c01"

sess = requests.Session()
sess.headers.update({"User-Agent":"Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36"})

# Portal login
sess.get("https://portal.jvision.ac.kr/user/loginAuth.face",
    params={"langKnd":"ko","userId":uid,"password":upw}, verify=False, timeout=10)
sess.post("https://portal.jvision.ac.kr/user/loginProcess.face",
    data={"langKnd":"ko","userId":uid,"username":uid,"password":upw}, verify=False, timeout=10)

# SSO bridge to LMS
sso_r = sess.get(
    "http://sso.jvision.ac.kr/enpass/login?gateway=client&epAppId=jvisionCommon&service=https://lms.jvision.ac.kr/ilos/main/main_form.acl",
    verify=False, allow_redirects=True, timeout=15)

print(f"SSO result: {sso_r.status_code}, URL: {sso_r.url}")
print(f"SSO redirect history:")
for r in sso_r.history:
    print(f"  {r.status_code} -> {r.url}")
    if 'Set-Cookie' in r.headers:
        print(f"    Set-Cookie: {r.headers.get('Set-Cookie', '')[:100]}")

print(f"\nAll cookies after SSO:")
for c in sess.cookies:
    print(f"  [{c.domain}] {c.name}={c.value[:30]}... (path={c.path})")

# Check if there's a JSESSIONID anywhere
print(f"\nLooking for JSESSIONID:")
for c in sess.cookies:
    if c.name == 'JSESSIONID':
        print(f"  Found in domain: {c.domain}, value: {c.value[:40]}...")

# Also check response headers for Set-Cookie
print(f"\nFinal response Set-Cookie: {sso_r.headers.get('Set-Cookie','none')}")
print(f"\n338-byte course list content:")
sess.get(f"{LMS}/ilos/mp/course_register_list_form.acl", verify=False, timeout=10)
r_c = sess.post(f"{LMS}/ilos/mp/course_register_list.acl",
    data={"YEAR":"2026","TERM":"1","num":"1","encoding":"utf-8"},
    headers={"Content-Type":"application/x-www-form-urlencoded; charset=UTF-8","X-Requested-With":"XMLHttpRequest",
             "Referer":f"{LMS}/ilos/mp/course_register_list_form.acl","Origin":LMS},
    verify=False, timeout=10)
print(r_c.text)
