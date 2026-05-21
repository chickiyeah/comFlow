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
    "Accept-Language":"ko,en-US;q=0.9",
})

sess.cookies.set("_language_", "ko", domain="lms.jvision.ac.kr", path="/")
sess.cookies.set("co_check", "1", domain="lms.jvision.ac.kr", path="/")
sess.cookies.set("EnviewLangKnd", "ko", domain="lms.jvision.ac.kr", path="/")

# Login form GET
sess.get(f"{LMS}/ilos/main/member/login_form.acl", verify=False, timeout=10)

# Login POST
r_login = sess.post(f"{LMS}/ilos/lo/login.acl",
    data={"returnURL":"","challenge":"","response":"","usr_id":uid,"usr_pwd":upw},
    headers={"Content-Type":"application/x-www-form-urlencoded",
             "Referer":f"{LMS}/ilos/main/member/login_form.acl","Origin":LMS,
             "Upgrade-Insecure-Requests":"1","Sec-Fetch-Dest":"document",
             "Sec-Fetch-Mode":"navigate","Sec-Fetch-Site":"same-origin","Sec-Fetch-User":"?1"},
    verify=False, timeout=15, allow_redirects=False)  # DON'T follow HTTP redirects
print(f"Login: {r_login.status_code}, size={len(r_login.content)}")

# Follow JavaScript redirect
js_redirect = re.search(r'document\.location\.href=["\']([^"\']+)["\']', r_login.text)
if js_redirect:
    redirect_url = js_redirect.group(1)
    if not redirect_url.startswith('http'):
        redirect_url = LMS + redirect_url
    print(f"JS redirect to: {redirect_url}")
    r_main = sess.get(redirect_url,
        headers={"Referer":f"{LMS}/ilos/lo/login.acl","Sec-Fetch-Dest":"document","Sec-Fetch-Mode":"navigate"},
        verify=False, timeout=15)
    print(f"Main page: {r_main.status_code}, size={len(r_main.content)}, URL={r_main.url}")
else:
    print("No JS redirect found!")
    print(r_login.text[:200])

print(f"LMS cookies: {[(c.name, c.value[:20], c.domain) for c in sess.cookies if 'lms' in c.domain]}")

# Course list
sess.get(f"{LMS}/ilos/mp/course_register_list_form.acl", verify=False, timeout=10)
r_c = sess.post(f"{LMS}/ilos/mp/course_register_list.acl",
    data={"YEAR":"2026","TERM":"1","num":"1","encoding":"utf-8"},
    headers={"Content-Type":"application/x-www-form-urlencoded; charset=UTF-8","X-Requested-With":"XMLHttpRequest",
             "Referer":f"{LMS}/ilos/mp/course_register_list_form.acl","Origin":LMS},
    verify=False, timeout=10)
print(f"\nCourse list: {len(r_c.content)} bytes")

# Submain POST (AFTER visiting main page)
r_sub = sess.post(f"{LMS}/ilos/st/course/submain_form.acl",
    data={"ky": KLASS},
    headers={"Content-Type":"application/x-www-form-urlencoded",
             "Referer":f"{LMS}/ilos/mp/course_register_list_form.acl",
             "Sec-Fetch-Dest":"document","Sec-Fetch-Mode":"navigate","Sec-Fetch-Site":"same-origin"},
    verify=False, timeout=10, allow_redirects=True)
print(f"\nSubmain: {r_sub.url}, size={len(r_sub.content)}")

n_vals = set(re.findall(r'N\d{4}[A-Z0-9]+c\d+', r_sub.text))
print(f"N-format IDs: {n_vals}")

# Attendance for all N-format IDs
for ky in (n_vals or {N_KY}):
    r_af = sess.get(f"{LMS}/ilos/st/course/attendance_list_form.acl",
        params={"ky":ky,"ud":uid}, verify=False, timeout=10)
    ra = sess.post(f"{LMS}/ilos/st/course/attendance_list.acl",
        data={"ud":uid,"ky":ky,"encoding":"utf-8"},
        headers={"Content-Type":"application/x-www-form-urlencoded; charset=UTF-8",
                 "X-Requested-With":"XMLHttpRequest",
                 "Referer":f"{LMS}/ilos/st/course/attendance_list_form.acl?ky={ky}","Origin":LMS},
        verify=False, timeout=15)
    if len(ra.content) > 200:
        print(f"\n*** SUCCESS ky={ky}! ***")
        print(ra.text[:600])
    else:
        print(f"\nAttend ky={ky}: size={len(ra.content)}: {ra.text[:80]}")
