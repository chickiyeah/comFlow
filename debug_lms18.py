import requests, urllib3, re
urllib3.disable_warnings()

uid = "201918023"
upw = "D@lstn!0722"
LMS = "https://lms.jvision.ac.kr"
KLASS = "A20261118031052182850c01"
N_KY = "N2026B182940c00"

sess = requests.Session()
sess.headers.update({"User-Agent":"Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/148.0.0.0 Safari/537.36",
                     "Accept-Language":"ko,en-US;q=0.9"})

# Manually set browser-like cookies BEFORE login (what browser has from prior visit)
sess.cookies.set("_language_", "ko", domain="lms.jvision.ac.kr", path="/")
sess.cookies.set("co_check", "1", domain="lms.jvision.ac.kr", path="/")
sess.cookies.set("EnviewLangKnd", "ko", domain="lms.jvision.ac.kr", path="/")

# Login form GET
sess.get(f"{LMS}/ilos/main/member/login_form.acl", verify=False, timeout=10)
print(f"After form GET cookies: {[(c.name, c.domain) for c in sess.cookies if 'lms' in c.domain]}")

# Login POST (with all browser-like cookies)
r_login = sess.post(f"{LMS}/ilos/lo/login.acl",
    data={"returnURL":"","challenge":"","response":"","usr_id":uid,"usr_pwd":upw},
    headers={"Content-Type":"application/x-www-form-urlencoded",
             "Referer":f"{LMS}/ilos/main/member/login_form.acl",
             "Origin":LMS,"Upgrade-Insecure-Requests":"1"},
    verify=False, timeout=15, allow_redirects=True)
print(f"Login: {r_login.status_code}, URL: {r_login.url}")
print(f"LMS cookies: {[(c.name, c.value[:20], c.domain) for c in sess.cookies if 'lms' in c.domain]}")

# Course list
sess.get(f"{LMS}/ilos/mp/course_register_list_form.acl", verify=False, timeout=10)
r_c = sess.post(f"{LMS}/ilos/mp/course_register_list.acl",
    data={"YEAR":"2026","TERM":"1","num":"1","encoding":"utf-8"},
    headers={"Content-Type":"application/x-www-form-urlencoded; charset=UTF-8","X-Requested-With":"XMLHttpRequest",
             "Referer":f"{LMS}/ilos/mp/course_register_list_form.acl","Origin":LMS},
    verify=False, timeout=10)
print(f"\nCourse list: {len(r_c.content)} bytes")

# Try submain GET with both ky and ud
r_sg = sess.get(f"{LMS}/ilos/st/course/submain_form.acl",
    params={"ky": KLASS, "ud": uid},
    headers={"Referer":f"{LMS}/ilos/mp/course_register_list_form.acl",
             "Sec-Fetch-Dest":"document","Sec-Fetch-Mode":"navigate","Sec-Fetch-User":"?1"},
    verify=False, timeout=10, allow_redirects=True)
print(f"Submain GET: {r_sg.status_code}, URL: {r_sg.url}, size: {len(r_sg.content)}")

# Try attendance
r_af = sess.get(f"{LMS}/ilos/st/course/attendance_list_form.acl",
    params={"ky": N_KY, "ud": uid},
    headers={"Referer":f"{LMS}/ilos/main/main_form.acl",
             "Sec-Fetch-Dest":"document","Sec-Fetch-Mode":"navigate"},
    verify=False, timeout=10)
print(f"Attend form: {r_af.status_code}, size: {len(r_af.content)}")

ra = sess.post(f"{LMS}/ilos/st/course/attendance_list.acl",
    data={"ud":uid,"ky":N_KY,"encoding":"utf-8"},
    headers={"Content-Type":"application/x-www-form-urlencoded; charset=UTF-8",
             "X-Requested-With":"XMLHttpRequest",
             "Referer":f"{LMS}/ilos/st/course/attendance_list_form.acl?ky={N_KY}&ud={uid}","Origin":LMS},
    verify=False, timeout=15)
print(f"Attend POST: {ra.status_code}, size: {len(ra.content)}")
if len(ra.content) > 200:
    print(f"SUCCESS! {ra.text[:600]}")
else:
    print(f"Response: {ra.text[:100]}")
