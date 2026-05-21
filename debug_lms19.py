import requests, urllib3, re
urllib3.disable_warnings()

uid = "201918023"
upw = "D@lstn!0722"
LMS = "https://lms.jvision.ac.kr"
KLASS = "A20261118031052182850c01"
N_KY = "N2026B182940c00"

sess = requests.Session()
sess.headers.update({
    "User-Agent":"Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/148.0.0.0 Safari/537.36",
    "Accept":"text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,*/*;q=0.8",
    "Accept-Language":"ko,en-US;q=0.9,en;q=0.8",
    "Accept-Encoding":"gzip, deflate, br",
    "Connection":"keep-alive",
})

# Pre-set LMS cookies (from prior visit)
sess.cookies.set("_language_", "ko", domain="lms.jvision.ac.kr", path="/")
sess.cookies.set("co_check", "1", domain="lms.jvision.ac.kr", path="/")
sess.cookies.set("EnviewLangKnd", "ko", domain="lms.jvision.ac.kr", path="/")

# Login form GET
sess.get(f"{LMS}/ilos/main/member/login_form.acl",
    headers={"Referer": f"{LMS}/ilos/main/main_form.acl",
             "Sec-Fetch-Dest":"document","Sec-Fetch-Mode":"navigate","Sec-Fetch-Site":"same-origin","Cache-Control":"max-age=0"},
    verify=False, timeout=10)
print(f"After form GET: {[(c.name, c.domain) for c in sess.cookies if 'lms' in c.domain]}")

# Login POST with FULL browser headers (including Sec-Fetch)
r_login = sess.post(f"{LMS}/ilos/lo/login.acl",
    data={"returnURL":"","challenge":"","response":"","usr_id":uid,"usr_pwd":upw},
    headers={
        "Content-Type":"application/x-www-form-urlencoded",
        "Referer":f"{LMS}/ilos/main/member/login_form.acl",
        "Origin":LMS,
        "Upgrade-Insecure-Requests":"1",
        "Sec-Fetch-Dest":"document",
        "Sec-Fetch-Mode":"navigate",
        "Sec-Fetch-Site":"same-origin",
        "Sec-Fetch-User":"?1",
        "Cache-Control":"max-age=0",
    },
    verify=False, timeout=15, allow_redirects=True)

print(f"\nLogin: status={r_login.status_code}, URL={r_login.url}, size={len(r_login.content)}")
print(f"Login response snippet: {r_login.text[:200]}")
print(f"LMS cookies: {[(c.name, c.value[:20], c.domain) for c in sess.cookies if 'lms' in c.domain]}")

# Check what page we're on - main dashboard or login?
if 'main_form.acl' in r_login.url or len(r_login.content) > 10000:
    print("LOGIN SUCCESS - on main page")
else:
    print("LOGIN MAY HAVE FAILED - checking...")

# Try course list
sess.get(f"{LMS}/ilos/mp/course_register_list_form.acl",
    headers={"Sec-Fetch-Dest":"document","Sec-Fetch-Mode":"navigate"}, verify=False, timeout=10)
r_c = sess.post(f"{LMS}/ilos/mp/course_register_list.acl",
    data={"YEAR":"2026","TERM":"1","num":"1","encoding":"utf-8"},
    headers={"Content-Type":"application/x-www-form-urlencoded; charset=UTF-8",
             "X-Requested-With":"XMLHttpRequest",
             "Referer":f"{LMS}/ilos/mp/course_register_list_form.acl","Origin":LMS},
    verify=False, timeout=10)
print(f"\nCourse list: {len(r_c.content)} bytes, eids={re.findall(chr(39).join(['r\"eclassRoom\\(', '([^', ']+)', '\\)\"']), r_c.text)}")

# Submain POST
r_sub = sess.post(f"{LMS}/ilos/st/course/submain_form.acl",
    data={"ky": KLASS},
    headers={"Content-Type":"application/x-www-form-urlencoded",
             "Referer":f"{LMS}/ilos/mp/course_register_list_form.acl",
             "Sec-Fetch-Dest":"document","Sec-Fetch-Mode":"navigate","Sec-Fetch-Site":"same-origin","Sec-Fetch-User":"?1"},
    verify=False, timeout=10, allow_redirects=True)
print(f"\nSubmain: {r_sub.url}, size={len(r_sub.content)}")
n_vals = set(re.findall(r'N\d{4}[A-Z0-9]+c\d+', r_sub.text))
print(f"N-format: {n_vals}")

# Attendance
for ky in (n_vals or {N_KY}):
    ra = sess.post(f"{LMS}/ilos/st/course/attendance_list.acl",
        data={"ud":uid,"ky":ky,"encoding":"utf-8"},
        headers={"Content-Type":"application/x-www-form-urlencoded; charset=UTF-8",
                 "X-Requested-With":"XMLHttpRequest",
                 "Referer":f"{LMS}/ilos/st/course/attendance_list_form.acl?ky={ky}","Origin":LMS},
        verify=False, timeout=15)
    if len(ra.content) > 200:
        print(f"\nSUCCESS ky={ky}! {ra.text[:500]}")
    else:
        print(f"\nAttend ky={ky}: {ra.status_code}, {len(ra.content)}B: {ra.text[:80]}")
