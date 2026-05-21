import requests, urllib3, re
urllib3.disable_warnings()

uid = "201918023"
upw = "D@lstn!0722"
LMS = "https://lms.jvision.ac.kr"
KLASS = "A20261118031052182850c01"

sess = requests.Session()
sess.headers.update({"User-Agent":"Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/148.0.0.0 Safari/537.36",
                     "Accept-Language":"ko,en-US;q=0.9,en;q=0.8"})

# Step 1: Portal SSO login (same as check.jvision.ac.kr flow)
print("[1] Portal login...")
sess.get("https://portal.jvision.ac.kr/user/loginAuth.face",
    params={"langKnd":"ko","userId":uid,"password":upw}, verify=False, timeout=10)
sess.post("https://portal.jvision.ac.kr/user/loginProcess.face",
    data={"langKnd":"ko","userId":uid,"username":uid,"password":upw}, verify=False, timeout=10)

# Step 2: SSO bridge to LMS (same as my.jvision.ac.kr flow but for lms)
print("[2] SSO bridge to LMS...")
sso_r = sess.get(
    "http://sso.jvision.ac.kr/enpass/login?gateway=client&epAppId=jvisionCommon&service=https://lms.jvision.ac.kr/ilos/main/main_form.acl",
    headers={"Referer":"https://portal.jvision.ac.kr/"}, verify=False, allow_redirects=True, timeout=15)
print(f"  SSO bridge: {sso_r.status_code}, URL: {sso_r.url}, size={len(sso_r.content)}")
lms_jid = None
for c in sess.cookies:
    if c.name == 'JSESSIONID' and 'lms.jvision' in c.domain:
        lms_jid = c.value
        break
print(f"  LMS JSESSIONID: {lms_jid[:20] if lms_jid else 'NONE'}...")
print(f"  All LMS cookies: {[(c.name,c.domain) for c in sess.cookies if 'lms.jvision' in c.domain]}")

# Step 3: LMS main page
print("[3] LMS main page...")
r_main = sess.get(f"{LMS}/ilos/main/main_form.acl", verify=False, timeout=10)
print(f"  Main: {r_main.status_code}, size={len(r_main.content)}, URL: {r_main.url}")

# Step 4: Course list
print("[4] Course list...")
sess.get(f"{LMS}/ilos/mp/course_register_list_form.acl", verify=False, timeout=10)
r_c = sess.post(f"{LMS}/ilos/mp/course_register_list.acl",
    data={"YEAR":"2026","TERM":"1","num":"1","encoding":"utf-8"},
    headers={"Content-Type":"application/x-www-form-urlencoded; charset=UTF-8","X-Requested-With":"XMLHttpRequest",
             "Referer":f"{LMS}/ilos/mp/course_register_list_form.acl","Origin":LMS},
    verify=False, timeout=10)
eids = re.findall(r"eclassRoom\('([^']+)'\)", r_c.text)
print(f"  Size={len(r_c.content)}, eclassRoom IDs: {eids}")

# Step 5: Submain
print("[5] Submain POST...")
r_sub = sess.post(f"{LMS}/ilos/st/course/submain_form.acl",
    data={"ky": KLASS},
    headers={"Content-Type":"application/x-www-form-urlencoded",
             "Referer":f"{LMS}/ilos/mp/course_register_list_form.acl"},
    verify=False, timeout=10, allow_redirects=True)
print(f"  URL: {r_sub.url}, size={len(r_sub.content)}")

n_vals = set(re.findall(r'N\d{4}[A-Z0-9]+c\d+', r_sub.text))
print(f"  N-format IDs: {n_vals}")

# Step 6: Attendance for found N-format IDs
for ky in n_vals:
    r_af = sess.get(f"{LMS}/ilos/st/course/attendance_list_form.acl",
        params={"ky":ky,"ud":uid}, verify=False, timeout=10)
    ra = sess.post(f"{LMS}/ilos/st/course/attendance_list.acl",
        data={"ud":uid,"ky":ky,"encoding":"utf-8"},
        headers={"Content-Type":"application/x-www-form-urlencoded; charset=UTF-8",
                 "X-Requested-With":"XMLHttpRequest",
                 "Referer":f"{LMS}/ilos/st/course/attendance_list_form.acl?ky={ky}","Origin":LMS},
        verify=False, timeout=15)
    print(f"\nAttendance ky={ky}: {ra.status_code}, size={len(ra.content)}")
    if len(ra.content) > 200:
        print(f"SUCCESS! First 500 chars: {ra.text[:500]}")
    else:
        print(f"Response: {ra.text[:150]}")
