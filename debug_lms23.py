import requests, urllib3, re
urllib3.disable_warnings()

uid = "201918023"
upw = "D@lstn!0722"
LMS = "https://lms.jvision.ac.kr"
KLASS = "A20261118031052182850c01"
N_KY = "N2026B182940c00"

sess = requests.Session()
sess.headers.update({"User-Agent":"Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36"})
sess.cookies.set("_language_", "ko", domain="lms.jvision.ac.kr", path="/")
sess.cookies.set("co_check", "1", domain="lms.jvision.ac.kr", path="/")

# Full login (with JS redirect follow)
sess.get(f"{LMS}/ilos/main/member/login_form.acl", verify=False, timeout=10)
r_login = sess.post(f"{LMS}/ilos/lo/login.acl",
    data={"returnURL":"","challenge":"","response":"","usr_id":uid,"usr_pwd":upw},
    headers={"Content-Type":"application/x-www-form-urlencoded","Referer":f"{LMS}/ilos/main/member/login_form.acl","Origin":LMS},
    verify=False, timeout=15, allow_redirects=False)
js_redir = re.search(r"document\.location\.href=['\"]([^'\"]+)['\"]", r_login.text)
if js_redir:
    main_url = js_redir.group(1)
    if not main_url.startswith('http'):
        main_url = LMS + main_url
    sess.get(main_url, verify=False, timeout=15)

# GET the course list FORM page (needed for eclassRoom AJAX)
sess.get(f"{LMS}/ilos/mp/course_register_list_form.acl",
    headers={"Referer":f"{LMS}/ilos/main/main_form.acl"}, verify=False, timeout=10)

# Course list (to get KLASS IDs)
r_c = sess.post(f"{LMS}/ilos/mp/course_register_list.acl",
    data={"YEAR":"2026","TERM":"1","num":"1","encoding":"utf-8"},
    headers={"Content-Type":"application/x-www-form-urlencoded; charset=UTF-8","X-Requested-With":"XMLHttpRequest",
             "Referer":f"{LMS}/ilos/mp/course_register_list_form.acl","Origin":LMS},
    verify=False, timeout=10)
print(f"Course list: {len(r_c.content)} bytes")

# ** CRITICAL: POST to eclass_room2.acl first (the missing AJAX step!) **
print(f"\n[eclass_room2.acl] POST (KJKEY={KLASS})...")
r_eclass = sess.post(f"{LMS}/ilos/st/course/eclass_room2.acl",
    data={"KJKEY": KLASS, "FLAG": "mp", "returnURI": "/ilos/st/course/submain_form.acl", "encoding": "utf-8"},
    headers={"Content-Type":"application/x-www-form-urlencoded; charset=UTF-8",
             "X-Requested-With":"XMLHttpRequest",
             "Referer":f"{LMS}/ilos/mp/course_register_list_form.acl","Origin":LMS},
    verify=False, timeout=10)
print(f"  Status: {r_eclass.status_code}, size: {len(r_eclass.content)}")
print(f"  Response: {r_eclass.text[:300]}")

# Now try submain_form.acl
print(f"\n[submain_form.acl] after eclass_room2...")
r_sub = sess.get(f"{LMS}/ilos/st/course/submain_form.acl",
    headers={"Referer":f"{LMS}/ilos/mp/course_register_list_form.acl",
             "Sec-Fetch-Dest":"document","Sec-Fetch-Mode":"navigate"},
    verify=False, timeout=10, allow_redirects=True)
print(f"  URL: {r_sub.url}, size: {len(r_sub.content)}")

n_vals = set(re.findall(r'N\d{4}[A-Z0-9]+c\d+', r_sub.text))
print(f"  N-format IDs: {n_vals}")

# Attendance
for ky in (n_vals or {N_KY}):
    r_af = sess.get(f"{LMS}/ilos/st/course/attendance_list_form.acl",
        params={"ky":ky,"ud":uid}, verify=False, timeout=10)
    print(f"\n[attendance_list_form.acl] ky={ky}: {r_af.status_code}, size={len(r_af.content)}")
    ra = sess.post(f"{LMS}/ilos/st/course/attendance_list.acl",
        data={"ud":uid,"ky":ky,"encoding":"utf-8"},
        headers={"Content-Type":"application/x-www-form-urlencoded; charset=UTF-8",
                 "X-Requested-With":"XMLHttpRequest",
                 "Referer":f"{LMS}/ilos/st/course/attendance_list_form.acl?ky={ky}","Origin":LMS},
        verify=False, timeout=15)
    if len(ra.content) > 200:
        print(f"  *** SUCCESS! size={len(ra.content)} ***")
        print(ra.text[:800])
    else:
        print(f"  size={len(ra.content)}: {ra.text[:100]}")
