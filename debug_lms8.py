import requests, urllib3, re
urllib3.disable_warnings()

uid = "201918023"
upw = "D@lstn!0722"
LMS = "https://lms.jvision.ac.kr"
KLASS = "A20261118031052182850c01"
N_KY = "N2026B182940c00"

# CLEAN session - no portal cookies
sess = requests.Session()
sess.headers.update({"User-Agent":"Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/148.0.0.0 Safari/537.36",
                     "Accept-Language":"ko,en-US;q=0.9,en;q=0.8"})

# Login
r = sess.post(f"{LMS}/ilos/lo/login.acl",
    data={"returnURL":"","challenge":"","response":"","usr_id":uid,"usr_pwd":upw},
    headers={"Content-Type":"application/x-www-form-urlencoded","Referer":f"{LMS}/ilos/main/member/login_form.acl","Origin":LMS},
    verify=False, timeout=15, allow_redirects=True)
jid = sess.cookies.get("JSESSIONID")
print(f"Logged in: JSESSIONID={jid[:20]}...")
print(f"All cookies: {[(c.name,c.domain) for c in sess.cookies]}")

# Navigate to course list form
sess.get(f"{LMS}/ilos/mp/course_register_list_form.acl", verify=False, timeout=10)

# Get courses
sess.post(f"{LMS}/ilos/mp/course_register_list.acl",
    data={"YEAR":"2026","TERM":"1","num":"1","encoding":"utf-8"},
    headers={"Content-Type":"application/x-www-form-urlencoded; charset=UTF-8",
             "X-Requested-With":"XMLHttpRequest",
             "Referer":f"{LMS}/ilos/mp/course_register_list_form.acl","Origin":LMS},
    verify=False, timeout=10)

# Navigate to course classroom (eclassRoom simulation with ud parameter)
print("\n[Try GET submain with ky+ud params]")
r_sub = sess.get(f"{LMS}/ilos/st/course/submain_form.acl",
    params={"ky": KLASS, "ud": uid},
    headers={"Referer":f"{LMS}/ilos/mp/course_register_list_form.acl"},
    verify=False, timeout=10, allow_redirects=True)
print(f"  URL: {r_sub.url}, Status: {r_sub.status_code}, Size: {len(r_sub.content)}")

# Try attendance form GET (with ky as path param)
print("\n[Try attendance form with correct ky]")
for ky in [N_KY, KLASS]:
    rform = sess.get(f"{LMS}/ilos/st/course/attendance_list_form.acl",
        params={"ky": ky, "ud": uid},
        headers={"Referer": r_sub.url if r_sub.url != f"{LMS}/ilos/st/course/submain_form.acl" else f"{LMS}/ilos/mp/course_register_list_form.acl"},
        verify=False, timeout=10)
    print(f"  Form GET ky={ky[:20]}: {rform.status_code}, size={len(rform.content)}")

    ra = sess.post(f"{LMS}/ilos/st/course/attendance_list.acl",
        data={"ud":uid,"ky":ky,"encoding":"utf-8"},
        headers={"Content-Type":"application/x-www-form-urlencoded; charset=UTF-8",
                 "X-Requested-With":"XMLHttpRequest",
                 "Referer":f"{LMS}/ilos/st/course/attendance_list_form.acl?ky={ky}&ud={uid}","Origin":LMS},
        verify=False, timeout=15)
    print(f"  Attend POST: {ra.status_code}, size={len(ra.content)}")
    if len(ra.content) > 200:
        print(f"  SUCCESS: {ra.text[:300]}")
    else:
        print(f"  Response: {ra.text[:100]}")
