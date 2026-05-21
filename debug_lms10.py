import requests, urllib3, re, json
urllib3.disable_warnings()

uid = "201918023"
upw = "D@lstn!0722"
LMS = "https://lms.jvision.ac.kr"
KLASS = "A20261118031052182850c01"

# First get portal cookies (quick login)
sess_p = requests.Session()
sess_p.headers.update({"User-Agent":"Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36"})
sess_p.get("https://portal.jvision.ac.kr/user/loginAuth.face",
    params={"langKnd":"ko","userId":uid,"password":upw}, verify=False, timeout=10)
sess_p.post("https://portal.jvision.ac.kr/user/loginProcess.face",
    data={"langKnd":"ko","userId":uid,"username":uid,"password":upw}, verify=False, timeout=10)
portal_cookies = [(c.name, c.value, c.domain, c.path) for c in sess_p.cookies]
print(f"Portal cookies: {[(n, d) for n, v, d, p in portal_cookies]}")

# LMS session WITH portal cookies
sess = requests.Session()
sess.headers.update({"User-Agent":"Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36"})
for name, val, domain, path in portal_cookies:
    sess.cookies.set(name, val, domain="lms.jvision.ac.kr", path="/")

# LMS login
r_login = sess.post(f"{LMS}/ilos/lo/login.acl",
    data={"returnURL":"","challenge":"","response":"","usr_id":uid,"usr_pwd":upw},
    headers={"Content-Type":"application/x-www-form-urlencoded","Referer":f"{LMS}/ilos/main/member/login_form.acl","Origin":LMS},
    verify=False, timeout=15, allow_redirects=True)
print(f"LMS login: {r_login.status_code}, URL: {r_login.url}")
jid = sess.cookies.get("JSESSIONID", domain="lms.jvision.ac.kr")
print(f"LMS JSESSIONID: {jid[:20] if jid else 'NONE'}...")

# Course list
sess.get(f"{LMS}/ilos/mp/course_register_list_form.acl", verify=False, timeout=10)
r_courses = sess.post(f"{LMS}/ilos/mp/course_register_list.acl",
    data={"YEAR":"2026","TERM":"1","num":"1","encoding":"utf-8"},
    headers={"Content-Type":"application/x-www-form-urlencoded; charset=UTF-8","X-Requested-With":"XMLHttpRequest",
             "Referer":f"{LMS}/ilos/mp/course_register_list_form.acl","Origin":LMS},
    verify=False, timeout=10)
eclass_ids = re.findall(r"eclassRoom\('([^']+)'\)", r_courses.text)
print(f"eclassRoom IDs: {eclass_ids}")

# POST to submain (WITH portal cookies seeded)
r_sub = sess.post(f"{LMS}/ilos/st/course/submain_form.acl",
    data={"ky": KLASS},
    headers={"Content-Type":"application/x-www-form-urlencoded","Referer":f"{LMS}/ilos/mp/course_register_list_form.acl"},
    verify=False, timeout=10, allow_redirects=True)
print(f"Submain: {r_sub.url}, size={len(r_sub.content)}")

n_vals = set(re.findall(r'N\d{4}[A-Z0-9]+c\d+', r_sub.text))
print(f"N-format IDs: {n_vals}")

# Save for inspection
with open("C:/Users/ruddls030/jvision/main_with_portal.html", "w", encoding="utf-8-sig", errors="replace") as f:
    f.write(r_sub.text)

# Try attendance with N-format IDs found in this page
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
        print(f"SUCCESS! {ra.text[:500]}")
    else:
        print(f"Response: {ra.text[:150]}")
