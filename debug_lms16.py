import requests, urllib3, re
urllib3.disable_warnings()

uid = "201918023"
upw = "D@lstn!0722"
LMS = "https://lms.jvision.ac.kr"
KLASS = "A20261118031052182850c01"
N_KY = "N2026B182940c00"

sess = requests.Session()
sess.headers.update({"User-Agent":"Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/148.0.0.0 Safari/537.36",
                     "Accept-Language":"ko,en-US;q=0.9,en;q=0.8"})

# CRITICAL FIX: GET login form FIRST (sets JSESSIONID + co_check=1)
print("[0] GET login form (sets JSESSIONID + co_check=1)...")
r_form = sess.get(f"{LMS}/ilos/main/member/login_form.acl",
    headers={"Referer": f"{LMS}/ilos/main/main_form.acl"},
    verify=False, timeout=10)
print(f"  status={r_form.status_code}, size={len(r_form.content)}")
print(f"  Cookies after form GET: {[(c.name,c.value[:20],c.domain) for c in sess.cookies if 'lms' in c.domain]}")

# Now POST login (with JSESSIONID already in cookies)
print("\n[1] LMS login POST (with form JSESSIONID)...")
r_login = sess.post(f"{LMS}/ilos/lo/login.acl",
    data={"returnURL":"","challenge":"","response":"","usr_id":uid,"usr_pwd":upw},
    headers={"Content-Type":"application/x-www-form-urlencoded",
             "Referer":f"{LMS}/ilos/main/member/login_form.acl",
             "Origin":LMS,
             "Upgrade-Insecure-Requests":"1",
             "Sec-Fetch-Dest":"document","Sec-Fetch-Mode":"navigate",
             "Sec-Fetch-Site":"same-origin","Sec-Fetch-User":"?1"},
    verify=False, timeout=15, allow_redirects=True)
print(f"  status={r_login.status_code}, URL={r_login.url}, size={len(r_login.content)}")
print(f"  LMS cookies: {[(c.name,c.value[:20],c.domain) for c in sess.cookies if 'lms' in c.domain]}")

lms_jid = next((c.value for c in sess.cookies if c.name=='JSESSIONID' and 'lms.jvision' in c.domain), None)
print(f"  LMS JSESSIONID: {lms_jid[:20] if lms_jid else 'NONE'}...")

# Course list (form GET first)
print("\n[2] Course list...")
sess.get(f"{LMS}/ilos/mp/course_register_list_form.acl", verify=False, timeout=10)
r_c = sess.post(f"{LMS}/ilos/mp/course_register_list.acl",
    data={"YEAR":"2026","TERM":"1","num":"1","encoding":"utf-8"},
    headers={"Content-Type":"application/x-www-form-urlencoded; charset=UTF-8","X-Requested-With":"XMLHttpRequest",
             "Referer":f"{LMS}/ilos/mp/course_register_list_form.acl","Origin":LMS},
    verify=False, timeout=10)
eids = re.findall(r'eclassRoom\(\'([^\']+)\'\)', r_c.text)
print(f"  Size: {len(r_c.content)}, eclassRoom: {eids}")

# Submain POST
print("\n[3] Submain...")
r_sub = sess.post(f"{LMS}/ilos/st/course/submain_form.acl",
    data={"ky": KLASS},
    headers={"Content-Type":"application/x-www-form-urlencoded",
             "Referer":f"{LMS}/ilos/mp/course_register_list_form.acl",
             "Sec-Fetch-Dest":"document","Sec-Fetch-Mode":"navigate"},
    verify=False, timeout=10, allow_redirects=True)
print(f"  URL: {r_sub.url}, size={len(r_sub.content)}")
n_vals = set(re.findall(r'N\d{4}[A-Z0-9]+c\d+', r_sub.text))
print(f"  N-format IDs: {n_vals}")

# Attendance
for ky in (n_vals or {N_KY}):
    r_af = sess.get(f"{LMS}/ilos/st/course/attendance_list_form.acl",
        params={"ky":ky,"ud":uid}, verify=False, timeout=10)
    print(f"\nAttend form GET ky={ky}: {r_af.status_code}, size={len(r_af.content)}")
    ra = sess.post(f"{LMS}/ilos/st/course/attendance_list.acl",
        data={"ud":uid,"ky":ky,"encoding":"utf-8"},
        headers={"Content-Type":"application/x-www-form-urlencoded; charset=UTF-8",
                 "X-Requested-With":"XMLHttpRequest",
                 "Referer":f"{LMS}/ilos/st/course/attendance_list_form.acl?ky={ky}","Origin":LMS},
        verify=False, timeout=15)
    print(f"  Attend POST: {ra.status_code}, size={len(ra.content)}")
    if len(ra.content) > 200:
        print(f"  SUCCESS! {ra.text[:500]}")
    else:
        print(f"  Response: {ra.text[:100]}")

