import requests, urllib3, re
urllib3.disable_warnings()

uid = "201918023"
upw = "D@lstn!0722"
LMS = "https://lms.jvision.ac.kr"

sess = requests.Session()
sess.headers.update({"User-Agent":"Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36"})

# Portal login
sess.get("https://portal.jvision.ac.kr/user/loginAuth.face",
    params={"langKnd":"ko","userId":uid,"password":upw}, verify=False, timeout=10)
sess.post("https://portal.jvision.ac.kr/user/loginProcess.face",
    data={"langKnd":"ko","userId":uid,"username":uid,"password":upw}, verify=False, timeout=10)

# SSO bridge to LMS (do NOT follow all redirects - track each step)
sso_r = sess.get(
    "http://sso.jvision.ac.kr/enpass/login?gateway=client&epAppId=jvisionCommon&service=https://lms.jvision.ac.kr/ilos/main/main_form.acl",
    verify=False, allow_redirects=False, timeout=15)  # Don't follow!
print(f"SSO step 1: {sso_r.status_code}")
for k, v in sso_r.headers.items():
    if k.lower() in ['location', 'set-cookie']:
        print(f"  {k}: {v[:100]}")

# Follow to LMS
if sso_r.status_code in (301, 302, 303):
    lms_url = sso_r.headers.get('Location', '')
    print(f"\nFollowing to: {lms_url[:100]}")

    lms_r = sess.get(lms_url, verify=False, allow_redirects=False, timeout=15)
    print(f"LMS step: {lms_r.status_code}")
    for k, v in lms_r.headers.items():
        if k.lower() in ['location', 'set-cookie']:
            print(f"  {k}: {v[:100]}")

    # If redirect again, follow
    while lms_r.status_code in (301, 302, 303, 307, 308):
        next_url = lms_r.headers.get('Location', '')
        if not next_url.startswith('http'):
            next_url = LMS + next_url
        print(f"\nFollowing to: {next_url[:100]}")
        lms_r = sess.get(next_url, verify=False, allow_redirects=False, timeout=15)
        print(f"  {lms_r.status_code}")
        for k, v in lms_r.headers.items():
            if k.lower() in ['location', 'set-cookie']:
                print(f"  {k}: {v[:150]}")

print(f"\nFinal cookies for lms.jvision.ac.kr:")
for c in sess.cookies:
    if 'lms' in c.domain:
        print(f"  {c.name}={c.value[:40]}... (domain={c.domain}, path={c.path})")

# Access protected page
print(f"\nAccess protected LMS page...")
r = sess.get(f"{LMS}/ilos/st/course/submain_form.acl?ky=A20261118031052182850c01&ud={uid}",
    headers={"Referer":f"{LMS}/ilos/main/main_form.acl"}, verify=False, timeout=10)
print(f"Submain: {r.status_code}, URL: {r.url}, size={len(r.content)}")
