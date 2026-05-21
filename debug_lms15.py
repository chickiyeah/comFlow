import requests, urllib3, re
urllib3.disable_warnings()

uid = "201918023"
upw = "D@lstn!0722"
LMS = "https://lms.jvision.ac.kr"
KLASS = "A20261118031052182850c01"
N_KY = "N2026B182940c00"

# Get fresh portal cookies
sess_p = requests.Session()
sess_p.headers.update({"User-Agent":"Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36"})
sess_p.get("https://portal.jvision.ac.kr/user/loginAuth.face",
    params={"langKnd":"ko","userId":uid,"password":upw}, verify=False, timeout=10)
sess_p.post("https://portal.jvision.ac.kr/user/loginProcess.face",
    data={"langKnd":"ko","userId":uid,"username":uid,"password":upw}, verify=False, timeout=10)

# Extract specific portal cookies
enpasstgc = None
enview_sid = None
for c in sess_p.cookies:
    if c.name == 'ENPASSTGC':
        enpasstgc = (c.name, c.value, c.domain, c.path)
    if c.name == 'EnviewSessionId' and '.jvision.ac.kr' in c.domain:
        enview_sid = (c.name, c.value, c.domain, c.path)

print(f"ENPASSTGC: {enpasstgc[1][:30] if enpasstgc else 'NONE'}...")
print(f"EnviewSessionId (.jvision.ac.kr): {enview_sid[1][:30] if enview_sid else 'NONE'}...")

# Build LMS session
sess = requests.Session()
sess.headers.update({"User-Agent":"Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/148.0.0.0 Safari/537.36"})

# Seed SSO cookies to lms domain
if enview_sid:
    sess.cookies.set("EnviewSessionId", enview_sid[1], domain="lms.jvision.ac.kr", path="/")
    sess.cookies.set("EnviewLangKnd", "ko", domain="lms.jvision.ac.kr", path="/")
if enpasstgc:
    sess.cookies.set(enpasstgc[0], enpasstgc[1], domain="sso.jvision.ac.kr", path="/enpass")

# Also seed to wildcard .jvision.ac.kr
if enview_sid:
    sess.cookies.set("EnviewSessionId", enview_sid[1], domain=".jvision.ac.kr", path="/")

# LMS login
print("\n[1] LMS direct login (with portal SSO cookies seeded)...")
r_login = sess.post(f"{LMS}/ilos/lo/login.acl",
    data={"returnURL":"","challenge":"","response":"","usr_id":uid,"usr_pwd":upw},
    headers={"Content-Type":"application/x-www-form-urlencoded","Referer":f"{LMS}/ilos/main/member/login_form.acl","Origin":LMS},
    verify=False, timeout=15, allow_redirects=True)
print(f"  Status: {r_login.status_code}, URL: {r_login.url}")
print(f"  LMS cookies: {[(c.name, c.domain) for c in sess.cookies if 'lms' in c.domain]}")

lms_jid = next((c.value for c in sess.cookies if c.name=='JSESSIONID' and 'lms.jvision' in c.domain), None)
print(f"  JSESSIONID: {lms_jid[:20] if lms_jid else 'NONE'}...")

# Course list
print("\n[2] Course list...")
sess.get(f"{LMS}/ilos/mp/course_register_list_form.acl", verify=False, timeout=10)
r_c = sess.post(f"{LMS}/ilos/mp/course_register_list.acl",
    data={"YEAR":"2026","TERM":"1","num":"1","encoding":"utf-8"},
    headers={"Content-Type":"application/x-www-form-urlencoded; charset=UTF-8","X-Requested-With":"XMLHttpRequest",
             "Referer":f"{LMS}/ilos/mp/course_register_list_form.acl","Origin":LMS},
    verify=False, timeout=10)
eids = re.findall(r"eclassRoom\('([^']+)'\)", r_c.text)
print(f"  Size: {len(r_c.content)}, eclassRoom IDs: {eids}")

# Submain
print("\n[3] Submain...")
r_sub = sess.post(f"{LMS}/ilos/st/course/submain_form.acl",
    data={"ky": KLASS},
    headers={"Content-Type":"application/x-www-form-urlencoded","Referer":f"{LMS}/ilos/mp/course_register_list_form.acl"},
    verify=False, timeout=10, allow_redirects=True)
print(f"  URL: {r_sub.url}, size={len(r_sub.content)}")
n_vals = set(re.findall(r'N\d{4}[A-Z0-9]+c\d+', r_sub.text))
print(f"  N-format IDs: {n_vals}")

# Attendance
for ky in (n_vals or {N_KY}):
    r_af = sess.get(f"{LMS}/ilos/st/course/attendance_list_form.acl",
        params={"ky":ky,"ud":uid}, verify=False, timeout=10)
    ra = sess.post(f"{LMS}/ilos/st/course/attendance_list.acl",
        data={"ud":uid,"ky":ky,"encoding":"utf-8"},
        headers={"Content-Type":"application/x-www-form-urlencoded; charset=UTF-8",
                 "X-Requested-With":"XMLHttpRequest",
                 "Referer":f"{LMS}/ilos/st/course/attendance_list_form.acl?ky={ky}","Origin":LMS},
        verify=False, timeout=15)
    print(f"\nAttend ky={ky}: {ra.status_code}, size={len(ra.content)}")
    if len(ra.content) > 200:
        print(f"SUCCESS! {ra.text[:400]}")
    else:
        print(f"Response: {ra.text[:150]}")
